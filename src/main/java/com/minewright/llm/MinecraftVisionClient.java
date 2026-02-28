package com.minewright.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewright.config.MineWrightConfig;
import com.minewright.exception.LLMClientException;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client for Minecraft screenshot analysis using vision models.
 *
 * <p><b>Vision Pipeline:</b></p>
 * <ol>
 *   <li>Try local vision model at localhost:8000 (free, fast)</li>
 *   <li>Fallback to glm-4.6v on z.ai (high quality, paid API)</li>
 *   <li>Both support base64-encoded PNG/JPEG images</li>
 * </ol>
 *
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li>"What do you see?" - General scene description</li>
 *   <li>"Is this area safe?" - Threat assessment</li>
 *   <li>"Where should I build?" - Terrain analysis</li>
 *   <li>"What resources are nearby?" - Resource detection</li>
 *   <li>"Am I going the right way?" - Navigation help</li>
 * </ul>
 *
 * <p><b>Local Vision Model Setup:</b></p>
 * <pre>
 * # Using vLLM (recommended):
 * vllm serve llava-hf/llava-1.5-7b-hf --port 8000
 *
 * # Using Ollama:
 * ollama run llava
 * # Then proxy to port 8000 or configure OLLAMA_VISION_URL
 * </pre>
 *
 * <p><b>Request Format:</b></p>
 * <pre>
 * {
 *   "image": "data:image/png;base64,iVBORw0KGgo...",
 *   "prompt": "What do you see in this Minecraft screenshot?",
 *   "analysis_type": "general" // or "threat", "terrain", "resources", "navigation"
 * }
 * </pre>
 *
 * <p><b>Response Format:</b></p>
 * <pre>
 * {
 *   "description": "A forest biome with oak trees...",
 *   "threats": ["zombie", "skeleton"],
 *   "resources": ["oak_log", "coal_ore"],
 *   "recommendations": ["Safe area", "Good for building"]
 * }
 * </pre>
 *
 * @since 2.0.0
 */
public class MinecraftVisionClient {
    private static final Logger LOGGER = TestLogger.getLogger(MinecraftVisionClient.class);

    // Vision model endpoints
    private static final String LOCAL_VISION_URL = "http://localhost:8000/v1/chat/completions";
    private static final String ZAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";
    private static final String GLM_4_6V_MODEL = "glm-4.6v";

    // Timeout settings
    private static final int LOCAL_TIMEOUT_SECONDS = 30;
    private static final int CLOUD_TIMEOUT_SECONDS = 60;

    // Retry settings
    private static final int MAX_RETRIES = 2;
    private static final int RETRY_DELAY_MS = 1000;

    // Failure tracking
    private final AtomicInteger localFailureCount = new AtomicInteger(0);
    private final AtomicInteger cloudFailureCount = new AtomicInteger(0);
    private static final int MAX_FAILURES_BEFORE_DISABLE = 5;

    private final HttpClient httpClient;
    private final String apiKey;
    private volatile boolean localAvailable = true;

    // Analysis type prompts
    private static final String GENERAL_SYSTEM_PROMPT = """
        You are a Minecraft vision analyzer. Describe what you see in the screenshot.
        Include terrain, biome, structures, mobs, and notable features.
        Be concise but thorough. Use JSON format with 'description' field.
        """;

    private static final String THREAT_SYSTEM_PROMPT = """
        You are a Minecraft threat analyzer. Assess dangers in the screenshot.
        Look for hostile mobs, environmental hazards (lava, cliffs), dark areas.
        Rate safety from 1-10 (10 = completely safe).
        Use JSON format: {'safety_rating': N, 'threats': ['list'], 'advice': 'text'}
        """;

    private static final String TERRAIN_SYSTEM_PROMPT = """
        You are a Minecraft terrain analyzer. Assess the area for building.
        Consider flatness, space, aesthetics, resources nearby.
        Rate build suitability from 1-10.
        Use JSON format: {'suitability': N, 'terrain_features': 'description', 'build_recommendations': ['list']}
        """;

    private static final String RESOURCES_SYSTEM_PROMPT = """
        You are a Minecraft resource detector. Identify valuable resources in the screenshot.
        Look for ore deposits, trees, crops, animals, chests.
        Estimate quantities and distances.
        Use JSON format: {'resources': [{'type': 'ore', 'amount': 'estimate', 'distance': 'estimate'}]}
        """;

    private static final String NAVIGATION_SYSTEM_PROMPT = """
        You are a Minecraft navigation assistant. Analyze the screenshot for route planning.
        Consider terrain, obstacles, landmarks, day/night cycle.
        Provide directional guidance.
        Use JSON format: {'direction': 'N/S/E/W/UP/DOWN', 'landmarks': 'description', 'route_advice': 'text'}
        """;

    /**
     * Analysis types for different vision tasks.
     */
    public enum AnalysisType {
        GENERAL("general", GENERAL_SYSTEM_PROMPT),
        THREAT("threat", THREAT_SYSTEM_PROMPT),
        TERRAIN("terrain", TERRAIN_SYSTEM_PROMPT),
        RESOURCES("resources", RESOURCES_SYSTEM_PROMPT),
        NAVIGATION("navigation", NAVIGATION_SYSTEM_PROMPT);

        private final String type;
        private final String systemPrompt;

        AnalysisType(String type, String systemPrompt) {
            this.type = type;
            this.systemPrompt = systemPrompt;
        }

        public String getType() { return type; }
        public String getSystemPrompt() { return systemPrompt; }
    }

    /**
     * Result from vision analysis.
     */
    public record VisionAnalysisResult(
        String description,
        String jsonResult,
        String modelUsed,
        long processingTimeMs,
        boolean fromCache
    ) {
        public static VisionAnalysisResult fromLocal(String description, String json, long time) {
            return new VisionAnalysisResult(description, json, "local-vision", time, false);
        }

        public static VisionAnalysisResult fromCloud(String description, String json, long time) {
            return new VisionAnalysisResult(description, json, GLM_4_6V_MODEL, time, false);
        }

        public static VisionAnalysisResult error(String error) {
            return new VisionAnalysisResult(error, "{}", "error", 0, false);
        }
    }

    public MinecraftVisionClient() {
        this.apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        // Check local availability on startup
        checkLocalAvailability();
    }

    /**
     * Analyzes a Minecraft screenshot with automatic fallback.
     *
     * <p>Flow: Try local vision model first (free, fast), then fallback to glm-4.6v</p>
     *
     * @param imageBase64 Base64-encoded PNG/JPEG image
     * @param userPrompt  Question about the image
     * @param analysisType Type of analysis (general, threat, terrain, etc.)
     * @return Analysis result with description and structured data
     */
    public VisionAnalysisResult analyzeScreenshot(String imageBase64, String userPrompt, AnalysisType analysisType) {
        long startTime = System.currentTimeMillis();

        // Validate input
        if (imageBase64 == null || imageBase64.isEmpty()) {
            LOGGER.error("[Vision] No image data provided");
            return VisionAnalysisResult.error("No image data provided");
        }

        // Clean base64 if it has data URL prefix
        String cleanBase64 = imageBase64;
        if (imageBase64.startsWith("data:image/")) {
            cleanBase64 = imageBase64.substring(imageBase64.indexOf(",") + 1);
        }

        LOGGER.info("[Vision] Analyzing screenshot with prompt: '{}'", userPrompt);

        // Try local vision model first
        if (localAvailable && localFailureCount.get() < MAX_FAILURES_BEFORE_DISABLE) {
            try {
                VisionAnalysisResult result = tryLocalVision(cleanBase64, userPrompt, analysisType);
                if (result != null) {
                    localFailureCount.set(0);
                    long time = System.currentTimeMillis() - startTime;
                    LOGGER.info("[Vision] Local analysis succeeded ({}ms)", time);
                    return result;
                }
            } catch (Exception e) {
                LOGGER.warn("[Vision] Local analysis failed: {}", e.getMessage());
                localFailureCount.incrementAndGet();
            }
        }

        // Fallback to glm-4.6v
        if (cloudFailureCount.get() < MAX_FAILURES_BEFORE_DISABLE) {
            try {
                VisionAnalysisResult result = tryCloudVision(cleanBase64, userPrompt, analysisType);
                if (result != null) {
                    cloudFailureCount.set(0);
                    long time = System.currentTimeMillis() - startTime;
                    LOGGER.info("[Vision] Cloud analysis succeeded ({}ms)", time);
                    return result;
                }
            } catch (Exception e) {
                LOGGER.error("[Vision] Cloud analysis failed: {}", e.getMessage());
                cloudFailureCount.incrementAndGet();
            }
        }

        long time = System.currentTimeMillis() - startTime;
        LOGGER.error("[Vision] All vision models failed after {}ms", time);
        return VisionAnalysisResult.error("All vision models unavailable. Please check local model or API key.");
    }

    /**
     * Asynchronously analyzes a screenshot.
     *
     * @param imageBase64 Base64-encoded image
     * @param userPrompt  Question about the image
     * @param analysisType Type of analysis
     * @return CompletableFuture with analysis result
     */
    public CompletableFuture<VisionAnalysisResult> analyzeScreenshotAsync(
            String imageBase64, String userPrompt, AnalysisType analysisType) {
        return CompletableFuture.supplyAsync(() ->
            analyzeScreenshot(imageBase64, userPrompt, analysisType)
        );
    }

    /**
     * Convenience method for "What do you see?" questions.
     */
    public String whatDoYouSee(String imageBase64) {
        VisionAnalysisResult result = analyzeScreenshot(imageBase64, "What do you see in this Minecraft screenshot?", AnalysisType.GENERAL);
        return result.description();
    }

    /**
     * Convenience method for threat assessment.
     */
    public String isAreaSafe(String imageBase64) {
        VisionAnalysisResult result = analyzeScreenshot(
            imageBase64,
            "Is this area safe? What threats are present?",
            AnalysisType.THREAT
        );
        return result.description();
    }

    /**
     * Convenience method for terrain analysis.
     */
    public String whereToBuild(String imageBase64) {
        VisionAnalysisResult result = analyzeScreenshot(
            imageBase64,
            "Where should I build here? What's the best location?",
            AnalysisType.TERRAIN
        );
        return result.description();
    }

    /**
     * Convenience method for resource detection.
     */
    public String whatResources(String imageBase64) {
        VisionAnalysisResult result = analyzeScreenshot(
            imageBase64,
            "What resources are visible in this screenshot?",
            AnalysisType.RESOURCES
        );
        return result.description();
    }

    /**
     * Convenience method for navigation help.
     */
    public String amIOnRightPath(String imageBase64) {
        VisionAnalysisResult result = analyzeScreenshot(
            imageBase64,
            "Am I going the right way? What direction should I go?",
            AnalysisType.NAVIGATION
        );
        return result.description();
    }

    /**
     * Tries the local vision model at localhost:8000.
     */
    private VisionAnalysisResult tryLocalVision(String imageBase64, String userPrompt, AnalysisType analysisType) {
        if (!localAvailable) {
            return null;
        }

        try {
            JsonObject requestBody = buildLocalVisionRequest(imageBase64, userPrompt, analysisType);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOCAL_VISION_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(LOCAL_TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                String result = parseLocalVisionResponse(response.body());
                LOGGER.info("[Vision] Local model responded in {}ms", latency);
                return VisionAnalysisResult.fromLocal(result, response.body(), latency);
            } else {
                LOGGER.warn("[Vision] Local model returned status {}: {}", response.statusCode(), response.body());
                localAvailable = false;
                return null;
            }
        } catch (Exception e) {
            LOGGER.debug("[Vision] Local model error: {}", e.getMessage());
            localAvailable = false;
            return null;
        }
    }

    /**
     * Tries the glm-4.6v cloud model via z.ai.
     */
    private VisionAnalysisResult tryCloudVision(String imageBase64, String userPrompt, AnalysisType analysisType) {
        if (apiKey == null || apiKey.isEmpty()) {
            LOGGER.error("[Vision] No API key configured for cloud vision");
            return null;
        }

        try {
            JsonObject requestBody = buildCloudVisionRequest(imageBase64, userPrompt, analysisType);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ZAI_API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(CLOUD_TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                String result = parseCloudVisionResponse(response.body());
                LOGGER.info("[Vision] Cloud model responded in {}ms", latency);
                return VisionAnalysisResult.fromCloud(result, response.body(), latency);
            } else {
                LOGGER.error("[Vision] Cloud model returned status {}: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("[Vision] Cloud model error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Builds request for local vision model (OpenAI-compatible format).
     */
    private JsonObject buildLocalVisionRequest(String imageBase64, String userPrompt, AnalysisType analysisType) {
        JsonObject body = new JsonObject();
        body.addProperty("model", "vision-model"); // Local model name
        body.addProperty("max_tokens", 1024);
        body.addProperty("temperature", 0.7);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", analysisType.getSystemPrompt());
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");

        JsonArray content = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", userPrompt);
        content.add(textPart);

        JsonObject imagePart = new JsonObject();
        imagePart.addProperty("type", "image_url");
        JsonObject imageUrl = new JsonObject();
        imageUrl.addProperty("url", "data:image/png;base64," + imageBase64);
        imagePart.add("image_url", imageUrl);
        content.add(imagePart);

        userMsg.add("content", content);
        messages.add(userMsg);

        body.add("messages", messages);

        return body;
    }

    /**
     * Builds request for glm-4.6v cloud model.
     */
    private JsonObject buildCloudVisionRequest(String imageBase64, String userPrompt, AnalysisType analysisType) {
        JsonObject body = new JsonObject();
        body.addProperty("model", GLM_4_6V_MODEL);
        body.addProperty("max_tokens", 2048);
        body.addProperty("temperature", 0.7);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", analysisType.getSystemPrompt());
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");

        JsonArray content = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", userPrompt);
        content.add(textPart);

        JsonObject imagePart = new JsonObject();
        imagePart.addProperty("type", "image_url");
        JsonObject imageUrl = new JsonObject();
        imageUrl.addProperty("url", "data:image/png;base64," + imageBase64);
        imagePart.add("image_url", imageUrl);
        content.add(imagePart);

        userMsg.add("content", content);
        messages.add(userMsg);

        body.add("messages", messages);

        return body;
    }

    /**
     * Parses response from local vision model.
     */
    private String parseLocalVisionResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("choices")) {
                JsonArray choices = json.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    if (firstChoice.has("message")) {
                        return firstChoice.getAsJsonObject("message").get("content").getAsString();
                    }
                }
            }
            return "Error: Invalid response format from local model";
        } catch (Exception e) {
            LOGGER.error("[Vision] Failed to parse local response: {}", e.getMessage());
            return "Error: Failed to parse local model response";
        }
    }

    /**
     * Parses response from glm-4.6v cloud model.
     */
    private String parseCloudVisionResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("choices")) {
                JsonArray choices = json.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    if (firstChoice.has("message")) {
                        return firstChoice.getAsJsonObject("message").get("content").getAsString();
                    }
                }
            }
            return "Error: Invalid response format from cloud model";
        } catch (Exception e) {
            LOGGER.error("[Vision] Failed to parse cloud response: {}", e.getMessage());
            return "Error: Failed to parse cloud model response";
        }
    }

    /**
     * Checks if local vision model is available.
     */
    private void checkLocalAvailability() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOCAL_VISION_URL.replace("/chat/completions", "/models")))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            localAvailable = response.statusCode() == 200;

            if (localAvailable) {
                LOGGER.info("[Vision] Local vision model available at {}", LOCAL_VISION_URL);
            } else {
                LOGGER.info("[Vision] Local vision model not available (status {}), will use cloud only", response.statusCode());
            }
        } catch (Exception e) {
            localAvailable = false;
            LOGGER.info("[Vision] Local vision model not reachable: {}", e.getMessage());
            LOGGER.info("[Vision] To enable local vision: vllm serve llava-hf/llava-1.5-7b-hf --port 8000");
        }
    }

    /**
     * Re-checks local availability (call periodically).
     */
    public void refreshLocalAvailability() {
        LOGGER.debug("[Vision] Refreshing local availability...");
        checkLocalAvailability();
        localFailureCount.set(0);
    }

    /**
     * Gets current status of vision backends.
     */
    public String getStatus() {
        return String.format(
            "VisionClient[local=%s, localFailures=%d, cloudFailures=%d, apiKeyConfigured=%s]",
            localAvailable ? "available" : "unavailable",
            localFailureCount.get(),
            cloudFailureCount.get(),
            apiKey != null && !apiKey.isEmpty()
        );
    }

    /**
     * Checks if any vision backend is available.
     */
    public boolean isAvailable() {
        return localAvailable || (apiKey != null && !apiKey.isEmpty());
    }
}
