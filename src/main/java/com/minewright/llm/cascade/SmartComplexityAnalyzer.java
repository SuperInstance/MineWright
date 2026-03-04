package com.minewright.llm.cascade;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewright.llm.LocalLLMClient;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

/**
 * Analyzes message complexity for intelligent LLM routing.
 *
 * <p><b>Analysis Methods:</b></p>
 * <ul>
 *   <li><b>Local Model Assessment:</b> Uses local SmolVLM when available</li>
 *   <li><b>Heuristic Fallback:</b> Pattern-based analysis when local unavailable</li>
 *   <li><b>Vision Detection:</b> Identifies when image processing is needed</li>
 *   <li><b>Complexity Classification:</b> TRIVIAL, SIMPLE, MODERATE, COMPLEX</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class SmartComplexityAnalyzer {
    private static final Logger LOGGER = TestLogger.getLogger(SmartComplexityAnalyzer.class);

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

    private final LocalLLMClient localLLM;

    public SmartComplexityAnalyzer(LocalLLMClient localLLM) {
        this.localLLM = localLLM;
    }

    /**
     * Analyzes a message to determine complexity and routing needs.
     *
     * @param message The user message to analyze
     * @return PreprocessResult with complexity assessment
     */
    public PreprocessResult analyze(String message) {
        // Try local SmolVLM for preprocessing
        if (localLLM.isAvailable()) {
            try {
                String localResponse = localLLM.sendRequest(COMPLEXITY_ASSESSMENT_PROMPT, message);
                if (localResponse != null) {
                    return parsePreprocessResult(localResponse, message);
                }
            } catch (Exception e) {
                LOGGER.debug("[SmartComplexity] Local preprocessing failed: {}", e.getMessage());
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
            LOGGER.warn("[SmartComplexity] Failed to parse preprocess result: {}", e.getMessage());
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
            case TRIVIAL, SIMPLE -> "smolvlm";
            case MODERATE -> "glm-4.7-flashx";
            case COMPLEX -> "glm-5";
            default -> "glm-4.7-flashx";
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

    /**
     * Result from preprocessing phase.
     */
    public record PreprocessResult(
        String cleanedMessage,
        String complexity,
        boolean needsVision,
        String recommendedModel,
        String reasoning
    ) {}
}
