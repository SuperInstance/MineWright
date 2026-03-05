package com.minewright.llm.cascade;

import com.minewright.config.MineWrightConfig;
import com.minewright.llm.LocalLLMClient;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

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
    // Dependencies
    // ------------------------------------------------------------------------

    private final SmartComplexityAnalyzer complexityAnalyzer;
    private final SmartModelRouter modelRouter;
    private final RouterMetrics metrics;
    private final ModelFailureTracker failureTracker;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Creates a new SmartCascadeRouter.
     */
    public SmartCascadeRouter() {
        String apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        LocalLLMClient localLLM = new LocalLLMClient();

        this.complexityAnalyzer = new SmartComplexityAnalyzer(localLLM);
        this.metrics = new RouterMetrics();
        this.failureTracker = new ModelFailureTracker();
        LLMAPIConnector apiConnector = new LLMAPIConnector(apiKey, localLLM);
        this.modelRouter = new SmartModelRouter(localLLM, apiConnector, metrics);

        logInitialization();
    }

    /**
     * Creates a new SmartCascadeRouter with custom dependencies (for testing).
     */
    public SmartCascadeRouter(SmartComplexityAnalyzer complexityAnalyzer,
                              SmartModelRouter modelRouter,
                              RouterMetrics metrics,
                              ModelFailureTracker failureTracker) {
        this.complexityAnalyzer = complexityAnalyzer;
        this.modelRouter = modelRouter;
        this.metrics = metrics;
        this.failureTracker = failureTracker;
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
        metrics.incrementTotalRequests();

        // Step 1: Check local availability and preprocess
        SmartComplexityAnalyzer.PreprocessResult preprocess = complexityAnalyzer.analyze(userMessage);

        LOGGER.info("[SmartCascade] Complexity: {}, Vision: {}, Recommended: {}, Message: {}",
            preprocess.complexity(),
            preprocess.needsVision(),
            preprocess.recommendedModel(),
            truncate(preprocess.cleanedMessage(), 50));

        // Step 2: Route based on assessment
        if (preprocess.needsVision()) {
            return processVisionRequest(systemPrompt, preprocess.cleanedMessage(), null);
        }

        return modelRouter.routeByComplexity(systemPrompt, preprocess.cleanedMessage(), preprocess);
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
        metrics.incrementVisionRequests();
        LOGGER.info("[SmartCascade] Processing vision request");

        // Check if vision model should be skipped
        if (failureTracker.shouldSkipModel(MODEL_VISION)) {
            LOGGER.warn("[SmartCascade] glm-4.6v in skip mode, trying other models");
            return processWithCascade(systemPrompt, userMessage + " [Image analysis requested but vision unavailable]");
        }

        try {
            String response = modelRouter.routeVisionRequest(systemPrompt, userMessage, imageBase64);
            failureTracker.recordSuccess(MODEL_VISION);
            return response;
        } catch (Exception e) {
            LOGGER.error("[SmartCascade] Vision processing failed: {}", e.getMessage());
            failureTracker.recordFailure(MODEL_VISION);
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
    // Metrics and Monitoring
    // ------------------------------------------------------------------------

    /**
     * Logs current router statistics.
     */
    public void logStats() {
        metrics.logStats();
    }

    /**
     * Gets the local hit rate.
     */
    public double getLocalHitRate() {
        return metrics.getLocalHitRate();
    }

    /**
     * Gets total requests processed.
     */
    public long getTotalRequests() {
        return metrics.getTotalRequests();
    }

    /**
     * Gets local hit count.
     */
    public long getLocalHits() {
        return metrics.getLocalHits();
    }

    /**
     * Gets cloud request count.
     */
    public long getCloudRequests() {
        return metrics.getCloudRequests();
    }

    /**
     * Resets all metrics.
     */
    public void resetMetrics() {
        metrics.reset();
        failureTracker.reset();
        LOGGER.info("SmartCascadeRouter metrics reset");
    }

    // ------------------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------------------

    private void logInitialization() {
        LOGGER.info("=== Smart Cascade Router Initialized ===");
        LOGGER.info("Local SmolVLM: localhost:8000 - AVAILABLE");
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
}
