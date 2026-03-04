package com.minewright.llm.async;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.http.HttpRequest;
import java.util.Map;

/**
 * Asynchronous Groq API client using Java HttpClient's sendAsync().
 *
 * <p>Groq provides BLAZING FAST inference on open-source LLMs (LLaMA, Mixtral).
 * Uses OpenAI-compatible API format for easy integration.</p>
 *
 * <p><b>API Endpoint:</b> https://api.groq.com/openai/v1/chat/completions</p>
 *
 * <p><b>Performance:</b> 0.5-2 seconds typical latency (much faster than OpenAI/Gemini)</p>
 *
 * <p><b>Supported Models:</b></p>
 * <ul>
 *   <li>llama-3.1-70b-versatile (best quality)</li>
 *   <li>llama-3.1-8b-instant (fastest, recommended)</li>
 *   <li>mixtral-8x7b-32768 (good balance)</li>
 *   <li>gemma-7b-it (Google's open model)</li>
 * </ul>
 *
 * <p><b>Free Tier Limits:</b></p>
 * <ul>
 *   <li>30 requests per minute</li>
 *   <li>14,400 requests per day</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Thread-safe. HttpClient is thread-safe and immutable.</p>
 *
 * @since 1.1.0
 */
public class AsyncGroqClient extends AbstractAsyncLLMClient {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String PROVIDER_ID = "groq";

    /**
     * Constructs an AsyncGroqClient.
     *
     * @param apiKey      Groq API key (required)
     * @param model       Model to use (e.g., "llama-3.1-8b-instant")
     * @param maxTokens   Maximum tokens in response (e.g., 500)
     * @param temperature Response randomness (0.0 - 2.0)
     * @throws IllegalArgumentException if apiKey is null or empty
     */
    public AsyncGroqClient(String apiKey, String model, int maxTokens, double temperature) {
        super(apiKey, model, maxTokens, temperature, PROVIDER_ID, 60000, 30);
    }

    @Override
    protected String getApiEndpoint() {
        return GROQ_API_URL;
    }

    @Override
    protected void addAuthHeaders(HttpRequest.Builder builder) {
        builder.header("Authorization", "Bearer " + apiKey);
    }

    @Override
    protected String buildRequestBody(String prompt, Map<String, Object> params) {
        JsonObject body = new JsonObject();

        String modelToUse = (String) params.getOrDefault("model", this.model);
        int maxTokensToUse = (int) params.getOrDefault("maxTokens", this.maxTokens);
        double tempToUse = (double) params.getOrDefault("temperature", this.temperature);

        body.addProperty("model", modelToUse);
        body.addProperty("max_tokens", maxTokensToUse);
        body.addProperty("temperature", tempToUse);

        JsonArray messages = new JsonArray();

        // System message
        String systemPrompt = (String) params.get("systemPrompt");
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemPrompt);
            messages.add(systemMessage);
        }

        // User message
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);

        body.add("messages", messages);

        return body.toString();
    }

    @Override
    protected LLMResponse parseResponse(String responseBody, long latencyMs) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (!json.has("choices") || json.getAsJsonArray("choices").isEmpty()) {
                throw new LLMException(
                        "Groq response missing 'choices' array",
                        LLMException.ErrorType.INVALID_RESPONSE,
                        PROVIDER_ID,
                        false
                );
            }

            JsonObject firstChoice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            String content = message.get("content").getAsString();

            int tokensUsed = 0;
            if (json.has("usage")) {
                JsonObject usage = json.getAsJsonObject("usage");
                tokensUsed = usage.get("total_tokens").getAsInt();
            }

            logger.debug("[groq] Response received (latency: {}ms, tokens: {})", latencyMs, tokensUsed);

            return LLMResponse.builder()
                    .content(content)
                    .model(model)
                    .providerId(PROVIDER_ID)
                    .latencyMs(latencyMs)
                    .tokensUsed(tokensUsed)
                    .fromCache(false)
                    .build();

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[groq] Failed to parse response: {}", truncate(responseBody, 200), e);
            throw new LLMException(
                    "Failed to parse Groq response: " + e.getMessage(),
                    LLMException.ErrorType.INVALID_RESPONSE,
                    PROVIDER_ID,
                    false,
                    e
            );
        }
    }
}
