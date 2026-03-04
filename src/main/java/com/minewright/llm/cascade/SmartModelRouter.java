package com.minewright.llm.cascade;

import com.minewright.llm.LocalLLMClient;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

/**
 * Routes LLM requests to appropriate models based on complexity assessment.
 *
 * <p><b>Routing Strategy:</b></p>
 * <ul>
 *   <li><b>TRIVIAL:</b> Local model only</li>
 *   <li><b>SIMPLE:</b> Local model preferred, cloud fallback</li>
 *   <li><b>MODERATE:</b> Try local, fallback to glm-4.7-flashx</li>
 *   <li><b>COMPLEX:</b> Route directly to GLM-5</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class SmartModelRouter {
    private static final Logger LOGGER = TestLogger.getLogger(SmartModelRouter.class);

    // Model identifiers
    public static final String LOCAL_SMOLVLM = "smolvlm";
    public static final String LOCAL_LLAMA = "llama3.2";
    public static final String MODEL_FLASHX = "glm-4.7-flashx";
    public static final String MODEL_FLASH = "glm-4.7-flash";
    public static final String MODEL_GLM5 = "glm-5";
    public static final String MODEL_VISION = "glm-4.6v";

    private final LocalLLMClient localLLM;
    private final LLMAPIConnector apiConnector;
    private final RouterMetrics metrics;

    public SmartModelRouter(LocalLLMClient localLLM, LLMAPIConnector apiConnector, RouterMetrics metrics) {
        this.localLLM = localLLM;
        this.apiConnector = apiConnector;
        this.metrics = metrics;
    }

    /**
     * Routes a text request based on complexity.
     *
     * @param systemPrompt System context
     * @param message User message
     * @param preprocess Complexity assessment result
     * @return Response from appropriate model
     */
    public String routeByComplexity(String systemPrompt, String message,
                                     SmartComplexityAnalyzer.PreprocessResult preprocess) {
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
     * Routes a vision request.
     *
     * @param systemPrompt System context
     * @param message User message
     * @param imageBase64 Base64-encoded image
     * @return Vision analysis response
     */
    public String routeVisionRequest(String systemPrompt, String message, String imageBase64) {
        metrics.incrementVisionRequests();
        LOGGER.info("[SmartRouter] Processing vision request");

        // Try local SmolVLM first (FREE!)
        if (localLLM.isAvailable()) {
            try {
                LOGGER.info("[SmartRouter] Trying local SmolVLM for vision - FREE!");
                String response = apiConnector.sendLocalVisionRequest(systemPrompt, message, imageBase64);
                if (response != null && !response.isEmpty()) {
                    metrics.incrementLocalHits();
                    metrics.incrementModelUsage(LOCAL_SMOLVLM);
                    LOGGER.info("[SmartRouter] Local SmolVLM vision succeeded - no API cost!");
                    return response;
                }
            } catch (Exception e) {
                LOGGER.warn("[SmartRouter] Local SmolVLM vision failed: {}", e.getMessage());
            }
        }

        // Fallback to glm-4.6v
        try {
            LOGGER.info("[SmartRouter] Falling back to glm-4.6v for vision");
            String response = apiConnector.sendCloudVisionRequest(systemPrompt, message, imageBase64, MODEL_VISION);
            metrics.incrementModelUsage(MODEL_VISION);
            return response;
        } catch (Exception e) {
            LOGGER.error("[SmartRouter] glm-4.6v vision failed: {}", e.getMessage());
            // Last resort: try with text-only
            return routeByComplexity(systemPrompt,
                message + " [Note: Image analysis failed, working from text description only]",
                new SmartComplexityAnalyzer.PreprocessResult(message, "MODERATE", false, "flashx", "Vision fallback"));
        }
    }

    /**
     * Handles TRIVIAL complexity - local only.
     */
    private String handleTrivial(String systemPrompt, String message) {
        LOGGER.info("[SmartRouter] TRIVIAL - using local model only");

        if (localLLM.isAvailable()) {
            try {
                String response = localLLM.sendRequest(systemPrompt, message);
                if (response != null && !response.isEmpty()) {
                    metrics.incrementLocalHits();
                    metrics.incrementModelUsage(LOCAL_LLAMA);
                    return response;
                }
            } catch (Exception e) {
                LOGGER.warn("[SmartRouter] Local trivial failed: {}", e.getMessage());
            }
        }

        // Fallback to flashx
        return tryCloudModel(systemPrompt, message, MODEL_FLASHX, 512);
    }

    /**
     * Handles SIMPLE complexity - local model preferred.
     */
    private String handleSimple(String systemPrompt, String message) {
        LOGGER.info("[SmartRouter] SIMPLE - trying local model first");

        if (localLLM.isAvailable()) {
            try {
                String response = localLLM.sendRequest(systemPrompt, message);
                if (response != null && !response.isEmpty()) {
                    metrics.incrementLocalHits();
                    metrics.incrementModelUsage(LOCAL_LLAMA);
                    return response;
                }
            } catch (Exception e) {
                LOGGER.debug("[SmartRouter] Local simple failed: {}", e.getMessage());
            }
        }

        // Fallback to flashx
        return tryFlashxThenFlash(systemPrompt, message);
    }

    /**
     * Handles MODERATE complexity - local with cloud fallback.
     */
    private String handleModerate(String systemPrompt, String message) {
        LOGGER.info("[SmartRouter] MODERATE - trying local, fallback to glm-4.7-flashx");

        // Try local first
        if (localLLM.isAvailable()) {
            try {
                String response = localLLM.sendRequest(systemPrompt, message);
                if (response != null && !response.isEmpty()) {
                    metrics.incrementLocalHits();
                    metrics.incrementModelUsage(LOCAL_LLAMA);
                    LOGGER.info("[SmartRouter] Local model succeeded for moderate task - FREE!");
                    return response;
                }
            } catch (Exception e) {
                LOGGER.debug("[SmartRouter] Local moderate failed: {}", e.getMessage());
            }
        }

        // Fallback chain: flashx -> flash -> glm5
        return tryFlashxThenFlashThenGlm5(systemPrompt, message);
    }

    /**
     * Handles COMPLEX complexity - route to GLM-5.
     */
    private String handleComplex(String systemPrompt, String message) {
        LOGGER.info("[SmartRouter] COMPLEX - routing to GLM-5");

        // Complex tasks go straight to GLM-5
        String result = tryCloudModel(systemPrompt, message, MODEL_GLM5, 8192);

        // If GLM-5 fails, try flash as fallback
        if (result.startsWith("Error:") || result.isEmpty()) {
            LOGGER.warn("[SmartRouter] GLM-5 failed, trying flash as fallback");
            result = tryCloudModel(systemPrompt, message, MODEL_FLASH, 8192);
        }

        return result;
    }

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
        return tryCloudModel(systemPrompt, message, MODEL_GLM5, 8192);
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
        try {
            metrics.incrementCloudRequests();
            metrics.incrementModelUsage(model);

            LOGGER.info("[SmartRouter] Trying cloud model: {}", model);
            String response = apiConnector.sendCloudRequest(systemPrompt, message, model, maxTokens);
            return response;
        } catch (Exception e) {
            LOGGER.error("[SmartRouter] Cloud model {} failed: {}", model, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
