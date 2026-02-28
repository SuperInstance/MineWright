package com.minewright.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewright.config.MineWrightConfig;
import com.minewright.llm.cascade.TaskComplexity;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Smart Cascade Router with intelligent multi-tier fallback strategy.
 *
 * <p><b>Routing Strategy:</b></p>
 * <ol>
 *   <li><b>First:</b> Try local SmolVLM (localhost:8000) - FREE and has vision</li>
 *   <li><b>Preprocessing:</b> Use local model to clean prompts and assess complexity</li>
 *   <li><b>Vision Tasks:</b> Route to local vision model first, fall back to glm-4.6v</li>
 *   <li><b>Complexity-Based Routing:</b>
 *     <ul>
 *       <li>TRIVIAL - local only</li>
 *       <li>SIMPLE - local model</li>
 *       <li>MODERATE - try local, fall back to glm-4.7-flashx</li>
 *       <li>COMPLEX - route to GLM-5</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p><b>Fallback Chain:</b></p>
 * <pre>
 * Local SmolVLM (localhost:8000) - FREE, fast, has vision
 *   ↓ (if unavailable or fails)
 * glm-4.7-flashx (z.ai) - fast cloud
 *   ↓ (if fails)
 * glm-4.7-flash (z.ai) - balanced cloud
 *   ↓ (if fails)
 * GLM-5 (z.ai) - best quality
 *   ↓ (for vision only)
 * glm-4.6v (z.ai) - complex vision when local fails
 * </pre>
 *
 * <p><b>Model Details:</b></p>
 * <ul>
 *   <li><b>SmolVLM</b>: Local vision model, 2B params, FREE</li>
 *   <li><b>glm-4.7-flashx</b>: Fastest cloud model, ~100ms latency</li>
 *   <li><b>glm-4.7-flash</b>: Balanced speed/quality, ~300ms latency</li>
 *   <li><b>GLM-5</b>: Best quality, ~1000ms latency</li>
 *   <li><b>glm-4.6v</b>: Vision model for complex image analysis</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class SmartCascadeRouter {
    private static final Logger LOGGER = TestLogger.getLogger(SmartCascadeRouter.class);

    // ------------------------------------------------------------------------
    // Model Identifiers
    // ------------------------------------------------------------------------

    // Local models
    public static final String LOCAL_SMOLVLM = "smolvlm";
    public static final String LOCAL_LLAMA = "llama3.2";

    // z.ai GLM models
    public static final String MODEL_GLM5 = "glm-5";
    public static final String MODEL_FLASHX = "glm-4.7-flashx";
    public static final String MODEL_FLASH = "glm-4.7-flash";
    public static final String MODEL_VISION = "glm-4.6v";

    // ------------------------------------------------------------------------
    // API Endpoints
    // ------------------------------------------------------------------------

    private static final String LOCAL_SMOLVLM_URL = "http://localhost:8000/v1/chat/completions";
    private static final String ZAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";

    // ------------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------------

    private static final int MAX_FAILURES_BEFORE_SKIP = 3;
    private static final long FAILURE_RESET_MS = 60000; // Reset after 1 minute
    private static final int MAX_LOCAL_TOKENS = 4096;
    private static final int MAX_CLOUD_TOKENS = 8192;

    // ------------------------------------------------------------------------
    // Dependencies
    // ------------------------------------------------------------------------

    private final LocalLLMClient localLLM;
    private final HttpClient client;
    private final String apiKey;

    // ------------------------------------------------------------------------
    // Failure Tracking
    // ------------------------------------------------------------------------

    private final ConcurrentHashMap<String, AtomicInteger> failureCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastFailureTime = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------------
    // Metrics
    // ------------------------------------------------------------------------

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong localHits = new AtomicLong(0);
    private final AtomicLocalLong cloudRequests = new AtomicLocalLong(0);
    private final AtomicLong visionRequests = new AtomicLong(0);
    private final AtomicLong totalCostCents = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> modelUsage = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------------
    // Prompts
    // ------------------------------------------------------------------------

    private static final String PREPROCESS_PROMPT = """
        You are a message preprocessor for a Minecraft AI assistant.
        Your job is to:
        1. Clean up the user's message (fix typos, clarify intent)
        2. Assess the complexity: TRIVIAL, SIMPLE, MODERATE, or COMPLEX
        3. Identify if vision is needed (screenshots/images)
        4. Recommend which model should handle this

        Respond in this exact JSON format:
        {
          "cleaned_message": "...",
          "complexity": "SIMPLE",
          "needs_vision": false,
          "recommended_model": "local",
          "reasoning": "Brief reason"
        }

        Guidelines:
        - TRIVIAL: greetings, follow, stop -> local only
        - SIMPLE: lookups, basic commands -> local model
        - MODERATE: resource gathering, simple builds -> try local, fallback to glm-4.7-flashx
        - COMPLEX: large builds, strategy, debugging -> route to GLM-5
        - needs_vision: true if screenshot/image analysis required
        - recommended_model: "local", "flashx", "flash", or "glm5"
        """;

    private static final String COMPLEXITY_ASSESSMENT_PROMPT = """
        Assess this Minecraft command's complexity and routing needs.
        Respond with JSON only:
        {
          "complexity": "TRIVIAL|SIMPLE|MODERATE|COMPLEX",
          "needs_vision": boolean,
          "recommended_model": "local|flashx|flash|glm5",
          "estimated_tokens": number,
          "reasoning": "brief explanation"
        }
        """;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Creates a new SmartCascadeRouter.
     */
    public SmartCascadeRouter() {
        this.apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.localLLM = new LocalLLMClient();

        // Initialize metrics
        modelUsage.put(LOCAL_SMOLVLM, new AtomicLong(0));
        modelUsage.put(LOCAL_LLAMA, new AtomicLong(0));
        modelUsage.put(MODEL_FLASHX, new AtomicLong(0));
        modelUsage.put(MODEL_FLASH, new AtomicLong(0));
        modelUsage.put(MODEL_GLM5, new AtomicLong(0));
        modelUsage.put(MODEL_VISION, new AtomicLong(0));

        logInitialization();
    }

    // ------------------------------------------------------------------------
    // Main Processing Methods
    // ------------------------------------------------------------------------

    /**
     * Processes a message with intelligent cascading routing.
     *
     * <p><b>Flow:</b></p>
     * <ol>
     *   <li>Check local SmolVLM availability</li>
     *   <li>Preprocess to assess complexity</li>
     *   <li>Route based on complexity and vision needs</li>
     *   <li>Fallback through chain on failure</li>
     * </ol>
     *
     * @param systemPrompt System context
     * @param userMessage  User's raw message
     * @return Response from the best available model
     */
    public String processWithCascade(String systemPrompt, String userMessage) {
        totalRequests.incrementAndGet();

        // Step 1: Check local availability and preprocess
        PreprocessResult preprocess = preprocessWithLocal(userMessage);

        LOGGER.info("[SmartCascade] Complexity: {}, Vision: {}, Recommended: {}, Message: {}",
            preprocess.complexity(),
            preprocess.needsVision(),
            preprocess.recommendedModel(),
            truncate(preprocess.cleanedMessage(), 50));

        // Step 2: Route based on assessment
        if (preprocess.needsVision()) {
            return processVisionRequest(systemPrompt, preprocess.cleanedMessage(), null);
        }

        return routeByComplexity(systemPrompt, preprocess.cleanedMessage(), preprocess);
    }

    /**
     * Processes a vision request with image.
     *
     * @param systemPrompt System context
     * @param userMessage  User's message
     * @param imageBase64  Base64-encoded image (can be null for screenshot capture)
     * @return Vision analysis response
     */
    public String processVisionRequest(String systemPrompt, String userMessage, String imageBase64) {
        visionRequests.incrementAndGet();
        LOGGER.info("[SmartCascade] Processing vision request");

        // Try local SmolVLM first (FREE!)
        if (localLLM.isAvailable()) {
            try {
                LOGGER.info("[SmartCascade] Trying local SmolVLM for vision - FREE!");
                String response = sendLocalVisionRequest(systemPrompt, userMessage, imageBase64);
                if (response != null && !response.isEmpty()) {
                    localHits.incrementAndGet();
                    modelUsage.get(LOCAL_SMOLVLM).incrementAndGet();
                    LOGGER.info("[SmartCascade] Local SmolVLM vision succeeded - no API cost!");
                    return response;
                }
            } catch (Exception e) {
                LOGGER.warn("[SmartCascade] Local SmolVLM vision failed: {}", e.getMessage());
            }
        }

        // Fallback to glm-4.6v
        if (shouldSkipModel(MODEL_VISION)) {
            LOGGER.warn("[SmartCascade] glm-4.6v in skip mode, trying other models");
            return processWithCascade(systemPrompt, userMessage + " [Image analysis requested but vision unavailable]");
        }

        try {
            LOGGER.info("[SmartCascade] Falling back to glm-4.6v for vision");
            String response = sendCloudVisionRequest(systemPrompt, userMessage, imageBase64, MODEL_VISION);
            recordSuccess(MODEL_VISION);
            modelUsage.get(MODEL_VISION).incrementAndGet();
            return response;
        } catch (Exception e) {
            LOGGER.error("[SmartCascade] glm-4.6v vision failed: {}", e.getMessage());
            recordFailure(MODEL_VISION);
            // Last resort: try with text-only
            return processWithCascade(systemPrompt,
                userMessage + " [Note: Image analysis failed, working from text description only]");
        }
    }

    /**
     * Processes a request asynchronously.
     */
    public CompletableFuture<String> processWithCascadeAsync(String systemPrompt, String userMessage) {
        return CompletableFuture.supplyAsync(() -> processWithCascade(systemPrompt, userMessage));
    }

    // ------------------------------------------------------------------------
    // Preprocessing and Complexity Assessment
    // ------------------------------------------------------------------------

    /**
     * Preprocesses message using local model to assess complexity.
     */
    private PreprocessResult preprocessWithLocal(String message) {
        // Try local SmolVLM for preprocessing
        if (localLLM.isAvailable()) {
            try {
                String localResponse = localLLM.sendRequest(COMPLEXITY_ASSESSMENT_PROMPT, message);
                if (localResponse != null) {
                    return parsePreprocessResult(localResponse, message);
                }
            } catch (Exception e) {
                LOGGER.debug("[SmartCascade] Local preprocessing failed: {}", e.getMessage());
            }
        }

        // Fallback to simple heuristic assessment
        return heuristicPreprocess(message);
    }

    /**
     * Parses preprocessing result from local model response.
     */
    private PreprocessResult parsePreprocessResult(String response, String originalMessage) {
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String cleaned = json.has("cleaned_message")
                ? json.get("cleaned_message").getAsString()
                : originalMessage;
            String complexity = json.has("complexity")
                ? json.get("complexity").getAsString()
                : "MODERATE";
            boolean needsVision = json.has("needs_vision")
                && json.get("needs_vision").getAsBoolean();
            String recommended = json.has("recommended_model")
                ? json.get("recommended_model").getAsString()
                : "local";
            String reasoning = json.has("reasoning")
                ? json.get("reasoning").getAsString()
                : "Local model assessment";

            return new PreprocessResult(cleaned, complexity, needsVision, recommended, reasoning);
        } catch (Exception e) {
            LOGGER.warn("[SmartCascade] Failed to parse preprocess result: {}", e.getMessage());
            return heuristicPreprocess(originalMessage);
        }
    }

    /**
     * Heuristic preprocessing when local model unavailable.
     */
    private PreprocessResult heuristicPreprocess(String message) {
        String lower = message.toLowerCase();

        // Check for vision keywords
        boolean needsVision = lower.contains("screenshot") ||
            lower.contains("look at") ||
            lower.contains("see") ||
            lower.contains("what's in front");

        // Assess complexity
        TaskComplexity complexity = assessComplexity(message);

        // Determine recommended model
        String recommended = switch (complexity) {
            case TRIVIAL, SIMPLE -> LOCAL_SMOLVLM;
            case MODERATE -> MODEL_FLASHX;
            case COMPLEX -> MODEL_GLM5;
            default -> MODEL_FLASHX;
        };

        return new PreprocessResult(message, complexity.name(), needsVision, recommended, "Heuristic assessment");
    }

    /**
     * Assesses task complexity using heuristics.
     */
    private TaskComplexity assessComplexity(String message) {
        String lower = message.toLowerCase();
        int wordCount = message.split("\\s+").length;

        // TRIVIAL: Single actions, well-known patterns
        if (wordCount <= 5 ||
            lower.matches(".*(follow|stop|wait|come|stay|hello|hi|hey).*")) {
            return TaskComplexity.TRIVIAL;
        }

        // SIMPLE: 1-2 actions, straightforward
        if (wordCount <= 15 ||
            lower.matches(".*(mine|craft|go to|place|break|collect).*")) {
            return TaskComplexity.SIMPLE;
        }

        // COMPLEX: Multi-step, coordination, debugging
        if (lower.matches(".*(build.*house|coordinate|strategy|debug|optimize|design).*") ||
            wordCount > 30) {
            return TaskComplexity.COMPLEX;
        }

        // Default to MODERATE
        return TaskComplexity.MODERATE;
    }

    // ------------------------------------------------------------------------
    // Routing Logic
    // ------------------------------------------------------------------------

    /**
     * Routes request based on complexity assessment.
     */
    private String routeByComplexity(String systemPrompt, String message, PreprocessResult preprocess) {
        TaskComplexity complexity = TaskComplexity.valueOf(preprocess.complexity());

        return switch (complexity) {
            case TRIVIAL -> handleTrivial(systemPrompt, message);
            case SIMPLE -> handleSimple(systemPrompt, message);
            case MODERATE -> handleModerate(systemPrompt, message);
            case COMPLEX -> handleComplex(systemPrompt, message);
            default -> handleModerate(systemPrompt, message);
        };
    }

    /**
     * Handles TRIVIAL complexity - local only.
     */
    private String handleTrivial(String systemPrompt, String message) {
        LOGGER.info("[SmartCascade] TRIVIAL - using local model only");

        if (localLLM.isAvailable()) {
            try {
                String response = localLLM.sendRequest(systemPrompt, message);
                if (response != null && !response.isEmpty()) {
                    localHits.incrementAndGet();
                    modelUsage.get(LOCAL_LLAMA).incrementAndGet();
                    return response;
                }
            } catch (Exception e) {
                LOGGER.warn("[SmartCascade] Local trivial failed: {}", e.getMessage());
            }
        }

        // Fallback to flashx
        return tryCloudModel(systemPrompt, message, MODEL_FLASHX, 512);
    }

    /**
     * Handles SIMPLE complexity - local model preferred.
     */
    private String handleSimple(String systemPrompt, String message) {
        LOGGER.info("[SmartCascade] SIMPLE - trying local model first");

        if (localLLM.isAvailable()) {
            try {
                String response = localLLM.sendRequest(systemPrompt, message);
                if (response != null && !response.isEmpty()) {
                    localHits.incrementAndGet();
                    modelUsage.get(LOCAL_LLAMA).incrementAndGet();
                    return response;
                }
            } catch (Exception e) {
                LOGGER.debug("[SmartCascade] Local simple failed: {}", e.getMessage());
            }
        }

        // Fallback to flashx
        return tryFlashxThenFlash(systemPrompt, message);
    }

    /**
     * Handles MODERATE complexity - local with cloud fallback.
     */
    private String handleModerate(String systemPrompt, String message) {
        LOGGER.info("[SmartCascade] MODERATE - trying local, fallback to glm-4.7-flashx");

        // Try local first
        if (localLLM.isAvailable()) {
            try {
                String response = localLLM.sendRequest(systemPrompt, message);
                if (response != null && !response.isEmpty()) {
                    localHits.incrementAndGet();
                    modelUsage.get(LOCAL_LLAMA).incrementAndGet();
                    LOGGER.info("[SmartCascade] Local model succeeded for moderate task - FREE!");
                    return response;
                }
            } catch (Exception e) {
                LOGGER.debug("[SmartCascade] Local moderate failed: {}", e.getMessage());
            }
        }

        // Fallback chain: flashx -> flash -> glm5
        return tryFlashxThenFlashThenGlm5(systemPrompt, message);
    }

    /**
     * Handles COMPLEX complexity - route to GLM-5.
     */
    private String handleComplex(String systemPrompt, String message) {
        LOGGER.info("[SmartCascade] COMPLEX - routing to GLM-5");

        // Complex tasks go straight to GLM-5
        String result = tryCloudModel(systemPrompt, message, MODEL_GLM5, MAX_CLOUD_TOKENS);

        // If GLM-5 fails, try flash as fallback
        if (result.startsWith("Error:") || result.isEmpty()) {
            LOGGER.warn("[SmartCascade] GLM-5 failed, trying flash as fallback");
            result = tryCloudModel(systemPrompt, message, MODEL_FLASH, MAX_CLOUD_TOKENS);
        }

        return result;
    }

    // ------------------------------------------------------------------------
    // Fallback Chains
    // ------------------------------------------------------------------------

    /**
     * Tries flashx, then flash, then glm5.
     */
    private String tryFlashxThenFlashThenGlm5(String systemPrompt, String message) {
        // Try flashx
        String result = tryCloudModel(systemPrompt, message, MODEL_FLASHX, 2048);
        if (!result.startsWith("Error:") && !result.isEmpty()) {
            return result;
        }

        // Try flash
        result = tryCloudModel(systemPrompt, message, MODEL_FLASH, 4096);
        if (!result.startsWith("Error:") && !result.isEmpty()) {
            return result;
        }

        // Try glm5
        return tryCloudModel(systemPrompt, message, MODEL_GLM5, MAX_CLOUD_TOKENS);
    }

    /**
     * Tries flashx, then flash.
     */
    private String tryFlashxThenFlash(String systemPrompt, String message) {
        String result = tryCloudModel(systemPrompt, message, MODEL_FLASHX, 1024);
        if (!result.startsWith("Error:") && !result.isEmpty()) {
            return result;
        }

        return tryCloudModel(systemPrompt, message, MODEL_FLASH, 2048);
    }

    /**
     * Tries a single cloud model with failure tracking.
     */
    private String tryCloudModel(String systemPrompt, String message, String model, int maxTokens) {
        if (shouldSkipModel(model)) {
            LOGGER.debug("[SmartCascade] Skipping {} due to recent failures", model);
            return "Error: Model " + model + " unavailable";
        }

        try {
            cloudRequests.incrementAndGet();
            modelUsage.get(model).incrementAndGet();

            LOGGER.info("[SmartCascade] Trying cloud model: {}", model);
            String response = sendCloudRequest(systemPrompt, message, model, maxTokens);
            recordSuccess(model);
            return response;
        } catch (Exception e) {
            LOGGER.error("[SmartCascade] Cloud model {} failed: {}", model, e.getMessage());
            recordFailure(model);
            return "Error: " + e.getMessage();
        }
    }

    // ------------------------------------------------------------------------
    // API Communication
    // ------------------------------------------------------------------------

    /**
     * Sends request to local SmolVLM for vision processing.
     */
    private String sendLocalVisionRequest(String systemPrompt, String userMessage, String imageBase64) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "smolvlm");
            requestBody.addProperty("max_tokens", MAX_LOCAL_TOKENS);
            requestBody.addProperty("temperature", 0.7);

            JsonArray messages = new JsonArray();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", systemPrompt);
                messages.add(systemMsg);
            }

            // Build multimodal content
            JsonArray content = new JsonArray();

            JsonObject textPart = new JsonObject();
            textPart.addProperty("type", "text");
            textPart.addProperty("text", userMessage);
            content.add(textPart);

            if (imageBase64 != null && !imageBase64.isEmpty()) {
                JsonObject imagePart = new JsonObject();
                imagePart.addProperty("type", "image_url");
                JsonObject imageUrl = new JsonObject();
                imageUrl.addProperty("url", "data:image/jpeg;base64," + imageBase64);
                imagePart.add("image_url", imageUrl);
                content.add(imagePart);
            }

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.add("content", content);
            messages.add(userMsg);

            requestBody.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOCAL_SMOLVLM_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseCloudResponse(response.body());
            } else {
                LOGGER.error("[SmartCascade] Local vision error {}: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("[SmartCascade] Local vision request failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sends request to cloud GLM model for vision processing.
     */
    private String sendCloudVisionRequest(String systemPrompt, String userMessage,
                                          String imageBase64, String model) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("API key not configured");
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", 4096);
        requestBody.addProperty("temperature", 0.7);

        JsonArray messages = new JsonArray();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            messages.add(systemMsg);
        }

        // Build multimodal content
        JsonArray content = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", userMessage);
        content.add(textPart);

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            JsonObject imagePart = new JsonObject();
            imagePart.addProperty("type", "image_url");
            JsonObject imageUrl = new JsonObject();
            imageUrl.addProperty("url", "data:image/jpeg;base64," + imageBase64);
            imagePart.add("image_url", imageUrl);
            content.add(imagePart);
        }

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
            throw new Exception("API returned status " + response.statusCode() + ": " + response.body());
        }

        return parseCloudResponse(response.body());
    }

    /**
     * Sends request to cloud GLM model.
     */
    private String sendCloudRequest(String systemPrompt, String userMessage,
                                     String model, int maxTokens) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("API key not configured");
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", maxTokens);
        requestBody.addProperty("temperature", 0.7);

        JsonArray messages = new JsonArray();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            messages.add(systemMsg);
        }

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

        return parseCloudResponse(response.body());
    }

    /**
     * Parses cloud API response.
     */
    private String parseCloudResponse(String responseBody) throws Exception {
        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
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

    // ------------------------------------------------------------------------
    // Failure Tracking
    // ------------------------------------------------------------------------

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

    // ------------------------------------------------------------------------
    // Metrics and Monitoring
    // ------------------------------------------------------------------------

    /**
     * Logs current router statistics.
     */
    public void logStats() {
        LOGGER.info("=== Smart Cascade Router Statistics ===");
        LOGGER.info("Total Requests: {}", totalRequests.get());
        LOGGER.info("Local Hits: {} ({:.1f}%)",
            localHits.get(),
            totalRequests.get() > 0 ? (localHits.get() * 100.0 / totalRequests.get()) : 0);
        LOGGER.info("Cloud Requests: {}", cloudRequests.get());
        LOGGER.info("Vision Requests: {}", visionRequests.get());
        LOGGER.info("Estimated Cost: ${:.2f}", totalCostCents.get() / 100.0);

        LOGGER.info("Model Usage:");
        modelUsage.forEach((model, count) -> {
            if (count.get() > 0) {
                LOGGER.info("  {}: {} requests", model, count.get());
            }
        });
    }

    /**
     * Gets the local hit rate.
     */
    public double getLocalHitRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) localHits.get() / total : 0.0;
    }

    /**
     * Gets total requests processed.
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * Gets local hit count.
     */
    public long getLocalHits() {
        return localHits.get();
    }

    /**
     * Gets cloud request count.
     */
    public long getCloudRequests() {
        return cloudRequests.get();
    }

    /**
     * Resets all metrics.
     */
    public void resetMetrics() {
        totalRequests.set(0);
        localHits.set(0);
        cloudRequests.set(0);
        visionRequests.set(0);
        totalCostCents.set(0);
        modelUsage.forEach((model, count) -> count.set(0));
        LOGGER.info("SmartCascadeRouter metrics reset");
    }

    // ------------------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------------------

    private void logInitialization() {
        LOGGER.info("=== Smart Cascade Router Initialized ===");
        LOGGER.info("Local SmolVLM: localhost:8000 - {}",
            localLLM.isAvailable() ? "AVAILABLE" : "NOT AVAILABLE");
        LOGGER.info("Cloud Models: glm-4.7-flashx, glm-4.7-flash, GLM-5, glm-4.6v");
        LOGGER.info("Strategy: Local first, cloud fallback");
        LOGGER.info("========================================");
    }

    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    // ------------------------------------------------------------------------
    // Inner Classes
    // ------------------------------------------------------------------------

    /**
     * Result from preprocessing phase.
     */
    private record PreprocessResult(
        String cleanedMessage,
        String complexity,
        boolean needsVision,
        String recommendedModel,
        String reasoning
    ) {}

    /**
     * Atomic long wrapper with proper serialization.
     */
    private static class AtomicLocalLong extends AtomicLong {
        private static final long serialVersionUID = 1L;

        public AtomicLocalLong(long initialValue) {
            super(initialValue);
        }
    }
}
