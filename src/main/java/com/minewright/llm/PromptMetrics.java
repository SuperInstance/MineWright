package com.minewright.llm;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks token usage and provides cost estimation for LLM API calls.
 *
 * <p>This utility class helps monitor and optimize prompt efficiency by:</p>
 * <ul>
 *   <li>Tracking total tokens used across all requests</li>
 *   <li>Estimating costs based on provider pricing</li>
 *   <li>Providing per-request metrics for logging</li>
 *   <li>Supporting both synchronous and async operations</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe and can be used across multiple threads.</p>
 *
 * @since 1.3.0
 */
public class PromptMetrics {

    // Pricing (per 1M tokens as of 2024)
    private static final double OPENAI_GPT4_INPUT = 30.0;
    private static final double OPENAI_GPT4_OUTPUT = 60.0;
    private static final double OPENAI_GPT35_TURBO_INPUT = 0.50;
    private static final double OPENAI_GPT35_TURBO_OUTPUT = 1.50;
    private static final double GROQ_MIXTRAL_INPUT = 0.24; // Llama 3 70B
    private static final double GROQ_MIXTRAL_OUTPUT = 0.24;
    private static final double GEMINI_FLASH_INPUT = 0.075;
    private static final double GEMINI_FLASH_OUTPUT = 0.30;

    // Token counters (thread-safe)
    private static final AtomicLong totalInputTokens = new AtomicLong(0);
    private static final AtomicLong totalOutputTokens = new AtomicLong(0);
    private static final AtomicLong totalRequests = new AtomicLong(0);

    // Current provider and model
    private static volatile String currentProvider = "openai";
    private static volatile String currentModel = "gpt-4o";

    /**
     * Records token usage from an LLM request.
     *
     * @param inputTokens  Number of input tokens sent
     * @param outputTokens Number of output tokens received
     */
    public static void recordRequest(int inputTokens, int outputTokens) {
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);
        totalRequests.incrementAndGet();
    }

    /**
     * Records token usage with automatic estimation from string content.
     *
     * @param systemPrompt System prompt content
     * @param userPrompt   User prompt content
     * @param response     Response content
     */
    public static void recordRequestFromContent(String systemPrompt, String userPrompt, String response) {
        int inputTokens = estimateTokens(systemPrompt) + estimateTokens(userPrompt);
        int outputTokens = estimateTokens(response);
        recordRequest(inputTokens, outputTokens);
    }

    /**
     * Estimates token count from text.
     * Uses a simple approximation: 1 token ≈ 4 characters for code/JSON-like text.
     *
     * @param text Text to estimate tokens for
     * @return Estimated token count
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Approximate: 1 token ≈ 4 characters for technical text
        // For natural language, it's closer to 1 token ≈ 3-4 characters
        // Using 4 as a conservative estimate for code/JSON
        return (text.length() + 3) / 4;
    }

    /**
     * Calculates estimated cost for a request based on current provider/model.
     *
     * @param inputTokens  Input tokens
     * @param outputTokens Output tokens
     * @return Estimated cost in USD
     */
    public static double calculateCost(int inputTokens, int outputTokens) {
        double inputCostPerToken = getInputCostPerToken();
        double outputCostPerToken = getOutputCostPerToken();

        return (inputTokens * inputCostPerToken) + (outputTokens * outputCostPerToken);
    }

    /**
     * Calculates estimated cost from text content.
     *
     * @param systemPrompt System prompt
     * @param userPrompt   User prompt
     * @param response     Response
     * @return Estimated cost in USD
     */
    public static double calculateCostFromContent(String systemPrompt, String userPrompt, String response) {
        int inputTokens = estimateTokens(systemPrompt) + estimateTokens(userPrompt);
        int outputTokens = estimateTokens(response);
        return calculateCost(inputTokens, outputTokens);
    }

    private static double getInputCostPerToken() {
        return switch (currentProvider.toLowerCase()) {
            case "openai" -> {
                if (currentModel.contains("gpt-4")) {
                    yield OPENAI_GPT4_INPUT / 1_000_000;
                } else {
                    yield OPENAI_GPT35_TURBO_INPUT / 1_000_000;
                }
            }
            case "groq" -> GROQ_MIXTRAL_INPUT / 1_000_000;
            case "gemini" -> GEMINI_FLASH_INPUT / 1_000_000;
            default -> OPENAI_GPT4_INPUT / 1_000_000;
        };
    }

    private static double getOutputCostPerToken() {
        return switch (currentProvider.toLowerCase()) {
            case "openai" -> {
                if (currentModel.contains("gpt-4")) {
                    yield OPENAI_GPT4_OUTPUT / 1_000_000;
                } else {
                    yield OPENAI_GPT35_TURBO_OUTPUT / 1_000_000;
                }
            }
            case "groq" -> GROQ_MIXTRAL_OUTPUT / 1_000_000;
            case "gemini" -> GEMINI_FLASH_OUTPUT / 1_000_000;
            default -> OPENAI_GPT4_OUTPUT / 1_000_000;
        };
    }

    /**
     * Sets the current provider and model for cost calculation.
     *
     * @param provider Provider name ("openai", "groq", "gemini")
     * @param model   Model name
     */
    public static void setProviderAndModel(String provider, String model) {
        currentProvider = provider;
        currentModel = model;
    }

    /**
     * Gets total input tokens used across all requests.
     */
    public static long getTotalInputTokens() {
        return totalInputTokens.get();
    }

    /**
     * Gets total output tokens generated across all requests.
     */
    public static long getTotalOutputTokens() {
        return totalOutputTokens.get();
    }

    /**
     * Gets total number of requests made.
     */
    public static long getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * Gets total estimated cost for all requests.
     */
    public static double getTotalEstimatedCost() {
        long input = totalInputTokens.get();
        long output = totalOutputTokens.get();
        return calculateCost((int) input, (int) output);
    }

    /**
     * Gets total tokens (input + output).
     */
    public static long getTotalTokens() {
        return totalInputTokens.get() + totalOutputTokens.get();
    }

    /**
     * Resets all metrics to zero.
     * Useful for testing or starting a new measurement period.
     */
    public static void reset() {
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        totalRequests.set(0);
    }

    /**
     * Generates a formatted summary of metrics.
     */
    public static String getSummary() {
        return String.format(
            "PromptMetrics{requests=%d, tokens=%d (in=%d, out=%d), cost=$%.4f, provider=%s, model=%s}",
            getTotalRequests(),
            getTotalTokens(),
            getTotalInputTokens(),
            getTotalOutputTokens(),
            getTotalEstimatedCost(),
            currentProvider,
            currentModel
        );
    }

    /**
     * Metrics for a single request.
     */
    public static class RequestMetrics {
        private final int inputTokens;
        private final int outputTokens;
        private final double estimatedCost;
        private final long timestamp;

        public RequestMetrics(int inputTokens, int outputTokens, double estimatedCost) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.estimatedCost = estimatedCost;
            this.timestamp = System.currentTimeMillis();
        }

        public int getInputTokens() {
            return inputTokens;
        }

        public int getOutputTokens() {
            return outputTokens;
        }

        public int getTotalTokens() {
            return inputTokens + outputTokens;
        }

        public double getEstimatedCost() {
            return estimatedCost;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format(
                "RequestMetrics{tokens=%d (in=%d, out=%d), cost=$%.6f}",
                getTotalTokens(),
                inputTokens,
                outputTokens,
                estimatedCost
            );
        }
    }

    /**
     * Creates RequestMetrics from content.
     */
    public static RequestMetrics createRequestMetrics(String systemPrompt, String userPrompt, String response) {
        int inputTokens = estimateTokens(systemPrompt) + estimateTokens(userPrompt);
        int outputTokens = estimateTokens(response);
        double cost = calculateCost(inputTokens, outputTokens);
        return new RequestMetrics(inputTokens, outputTokens, cost);
    }
}
