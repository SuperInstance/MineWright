package com.minewright.llm.cascade;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewright.llm.LocalLLMClient;
import com.minewright.testutil.TestLogger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;

/**
 * Handles API communication with local and cloud LLM providers.
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Local vision requests (SmolVLM on localhost:8000)</li>
 *   <li>Cloud vision requests (glm-4.6v)</li>
 *   <li>Cloud text requests (glm-4.7-flashx, glm-4.7-flash, GLM-5)</li>
 *   <li>Response parsing and validation</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class LLMAPIConnector {
    private static final Logger LOGGER = TestLogger.getLogger(LLMAPIConnector.class);

    private static final String LOCAL_SMOLVLM_URL = "http://localhost:8000/v1/chat/completions";
    private static final String ZAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";
    private static final int MAX_LOCAL_TOKENS = 4096;
    private static final int MAX_CLOUD_TOKENS = 8192;

    private final HttpClient client;
    private final String apiKey;
    private final LocalLLMClient localLLM;

    public LLMAPIConnector(String apiKey, LocalLLMClient localLLM) {
        this.apiKey = apiKey;
        this.localLLM = localLLM;
        this.client = HttpClient.newBuilder().
            connectTimeout(Duration.ofSeconds(30)).
            build();
    }

    /**
     * Sends request to local SmolVLM for vision processing.
     */
    public String sendLocalVisionRequest(String systemPrompt, String userMessage, String imageBase64) {
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

            HttpRequest request = HttpRequest.newBuilder().
                uri(URI.create(LOCAL_SMOLVLM_URL)).
                header("Content-Type", "application/json").
                timeout(Duration.ofSeconds(60)).
                POST(HttpRequest.BodyPublishers.ofString(requestBody.toString())).
                build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseCloudResponse(response.body());
            } else {
                LOGGER.error("[LLMAPI] Local vision error {}: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("[LLMAPI] Local vision request failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sends request to cloud GLM model for vision processing.
     */
    public String sendCloudVisionRequest(String systemPrompt, String userMessage,
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

        HttpRequest request = HttpRequest.newBuilder().
            uri(URI.create(ZAI_API_URL)).
            header("Authorization", "Bearer " + apiKey).
            header("Content-Type", "application/json").
            timeout(Duration.ofSeconds(120)).
            POST(HttpRequest.BodyPublishers.ofString(requestBody.toString())).
            build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("API returned status " + response.statusCode() + ": " + response.body());
        }

        return parseCloudResponse(response.body());
    }

    /**
     * Sends request to cloud GLM model.
     */
    public String sendCloudRequest(String systemPrompt, String userMessage,
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

        HttpRequest request = HttpRequest.newBuilder().
            uri(URI.create(ZAI_API_URL)).
            header("Authorization", "Bearer " + apiKey).
            header("Content-Type", "application/json").
            timeout(Duration.ofSeconds(120)).
            POST(HttpRequest.BodyPublishers.ofString(requestBody.toString())).
            build();

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
}
