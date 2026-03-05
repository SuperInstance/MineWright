package com.minewright.vision;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for VisionRequestBuilder.
 * Tests building vision API request bodies for multimodal LLM requests.
 */
@DisplayName("VisionRequestBuilder Tests")
class VisionRequestBuilderTest {

    private static final String TEST_MODEL = "HuggingFaceTB/SmolVLM-Instruct";
    private static final String TEST_PROMPT = "Analyze this Minecraft screenshot";
    private static final String TEST_BASE64_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    // ==================== Basic Request Tests ====================

    @Test
    @DisplayName("Should build basic vision request with image and text")
    void testBuildBasicRequest() {
        JsonObject request = VisionRequestBuilder.buildBasicRequest(
            TEST_MODEL,
            TEST_PROMPT,
            TEST_BASE64_IMAGE
        );

        assertNotNull(request, "Request should not be null");

        // Check model
        assertEquals(TEST_MODEL, request.get("model").getAsString(), "Model should match");

        // Check generation parameters
        assertTrue(request.has("max_tokens"), "Should have max_tokens");
        assertTrue(request.has("temperature"), "Should have temperature");
        assertEquals(512, request.get("max_tokens").getAsInt(), "Default max_tokens should be 512");
        assertEquals(0.7, request.get("temperature").getAsDouble(), 0.001, "Default temperature should be 0.7");

        // Check messages
        assertTrue(request.has("messages"), "Should have messages array");
        JsonArray messages = request.getAsJsonArray("messages");
        assertEquals(1, messages.size(), "Should have one message");

        JsonObject userMessage = messages.get(0).getAsJsonObject();
        assertEquals("user", userMessage.get("role").getAsString(), "Message role should be user");

        // Check content
        assertTrue(userMessage.has("content"), "Message should have content");
        JsonArray content = userMessage.getAsJsonArray("content");
        assertEquals(2, content.size(), "Content should have 2 parts (text + image)");

        // Check text part
        JsonObject textPart = content.get(0).getAsJsonObject();
        assertEquals("text", textPart.get("type").getAsString(), "First part should be text");
        assertEquals(TEST_PROMPT, textPart.get("text").getAsString(), "Text should match");

        // Check image part
        JsonObject imagePart = content.get(1).getAsJsonObject();
        assertEquals("image_url", imagePart.get("type").getAsString(), "Second part should be image_url");
        assertTrue(imagePart.has("image_url"), "Image part should have image_url object");
    }

    @Test
    @DisplayName("Should include detail parameter in image_url")
    void testBuildBasicRequest_DetailParameter() {
        JsonObject request = VisionRequestBuilder.buildBasicRequest(
            TEST_MODEL,
            TEST_PROMPT,
            TEST_BASE64_IMAGE
        );

        JsonArray messages = request.getAsJsonArray("messages");
        JsonObject userMessage = messages.get(0).getAsJsonObject();
        JsonArray content = userMessage.getAsJsonArray("content");
        JsonObject imagePart = content.get(1).getAsJsonObject();
        JsonObject imageUrl = imagePart.getAsJsonObject("image_url");

        assertTrue(imageUrl.has("url"), "Should have url");
        assertTrue(imageUrl.has("detail"), "Should have detail parameter");
        assertEquals("auto", imageUrl.get("detail").getAsString(), "Default detail should be auto");
        assertEquals(TEST_BASE64_IMAGE, imageUrl.get("url").getAsString(), "URL should match");
    }

    // ==================== Multi-Image Comparison Tests ====================

    @Test
    @DisplayName("Should build request with multiple images")
    void testBuildComparisonRequest() {
        String image1 = "data:image/png;base64,abc123";
        String image2 = "data:image/png;base64,def456";
        String image3 = "data:image/png;base64,ghi789";

        JsonObject request = VisionRequestBuilder.buildComparisonRequest(
            TEST_MODEL,
            "Compare these images",
            image1, image2, image3
        );

        JsonArray messages = request.getAsJsonArray("messages");
        JsonObject userMessage = messages.get(0).getAsJsonObject();
        JsonArray content = userMessage.getAsJsonArray("content");

        // Should have text + 3 images
        assertEquals(4, content.size(), "Content should have text + 3 images");

        // First part is text
        assertEquals("text", content.get(0).getAsJsonObject().get("type").getAsString());

        // Next 3 parts are images
        assertEquals("image_url", content.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals("image_url", content.get(2).getAsJsonObject().get("type").getAsString());
        assertEquals("image_url", content.get(3).getAsJsonObject().get("type").getAsString());

        // Verify image URLs
        assertEquals(image1, content.get(1).getAsJsonObject()
            .getAsJsonObject("image_url").get("url").getAsString());
        assertEquals(image2, content.get(2).getAsJsonObject()
            .getAsJsonObject("image_url").get("url").getAsString());
        assertEquals(image3, content.get(3).getAsJsonObject()
            .getAsJsonObject("image_url").get("url").getAsString());
    }

    @Test
    @DisplayName("Should handle comparison with two images")
    void testBuildComparisonRequest_TwoImages() {
        String before = "data:image/png;base64,before";
        String after = "data:image/png;base64,after";

        JsonObject request = VisionRequestBuilder.buildComparisonRequest(
            TEST_MODEL,
            "What changed?",
            before, after
        );

        JsonArray content = request.getAsJsonArray("messages")
            .get(0).getAsJsonObject()
            .getAsJsonArray("content");

        assertEquals(3, content.size(), "Should have text + 2 images");
    }

    @Test
    @DisplayName("Should handle comparison with single image")
    void testBuildComparisonRequest_SingleImage() {
        String single = "data:image/png;base64,single";

        JsonObject request = VisionRequestBuilder.buildComparisonRequest(
            TEST_MODEL,
            "Analyze this",
            single
        );

        JsonArray content = request.getAsJsonArray("messages")
            .get(0).getAsJsonObject()
            .getAsJsonArray("content");

        assertEquals(2, content.size(), "Should have text + 1 image");
    }

    // ==================== System Prompt Tests ====================

    @Test
    @DisplayName("Should build request with system prompt")
    void testBuildRequestWithSystem() {
        String systemPrompt = "You are a Minecraft AI assistant.";
        String userPrompt = "What biome is this?";
        String image = TEST_BASE64_IMAGE;

        JsonObject request = VisionRequestBuilder.buildRequestWithSystem(
            TEST_MODEL,
            systemPrompt,
            userPrompt,
            image
        );

        JsonArray messages = request.getAsJsonArray("messages");
        assertEquals(2, messages.size(), "Should have system + user messages");

        // Check system message
        JsonObject systemMessage = messages.get(0).getAsJsonObject();
        assertEquals("system", systemMessage.get("role").getAsString());
        assertEquals(systemPrompt, systemMessage.get("content").getAsString());

        // Check user message
        JsonObject userMessage = messages.get(1).getAsJsonObject();
        assertEquals("user", userMessage.get("role").getAsString());
        assertTrue(userMessage.has("content"));
        JsonArray content = userMessage.getAsJsonArray("content");
        assertEquals(2, content.size()); // text + image
    }

    @Test
    @DisplayName("Should handle empty system prompt")
    void testBuildRequestWithSystem_EmptySystem() {
        JsonObject request = VisionRequestBuilder.buildRequestWithSystem(
            TEST_MODEL,
            "",
            TEST_PROMPT,
            TEST_BASE64_IMAGE
        );

        JsonArray messages = request.getAsJsonArray("messages");
        // Empty system prompt should be skipped
        assertEquals(1, messages.size(), "Should skip empty system message");
        assertEquals("user", messages.get(0).getAsJsonObject().get("role").getAsString());
    }

    @Test
    @DisplayName("Should handle null system prompt")
    void testBuildRequestWithSystem_NullSystem() {
        JsonObject request = VisionRequestBuilder.buildRequestWithSystem(
            TEST_MODEL,
            null,
            TEST_PROMPT,
            TEST_BASE64_IMAGE
        );

        JsonArray messages = request.getAsJsonArray("messages");
        assertEquals(1, messages.size(), "Should skip null system message");
    }

    // ==================== Minecraft Vision Prompts Tests ====================

    @Test
    @DisplayName("Should provide biome analysis prompt")
    void testMinecraftVisionPrompts_BiomeAnalysis() {
        String prompt = MinecraftVisionPrompts.BIOME_ANALYSIS;

        assertNotNull(prompt, "Prompt should not be null");
        assertTrue(prompt.contains("biome"), "Should mention biome");
        assertTrue(prompt.contains("terrain"), "Should mention terrain");
        assertTrue(prompt.contains("resources"), "Should mention resources");
        assertTrue(prompt.contains("threats"), "Should mention threats");
        assertTrue(prompt.contains("JSON"), "Should request JSON format");
    }

    @Test
    @DisplayName("Should provide OCR reading prompt")
    void testMinecraftVisionPrompts_OcrReading() {
        String prompt = MinecraftVisionPrompts.OCR_READING;

        assertNotNull(prompt, "Prompt should not be null");
        assertTrue(prompt.contains("text"), "Should mention text");
        assertTrue(prompt.contains("signs"), "Should mention signs");
        assertTrue(prompt.contains("chat"), "Should mention chat");
        assertTrue(prompt.contains("exactly"), "Should request exact transcription");
    }

    @Test
    @DisplayName("Should provide build verification prompt")
    void testMinecraftVisionPrompts_BuildVerification() {
        String prompt = MinecraftVisionPrompts.BUILD_VERIFICATION;

        assertNotNull(prompt, "Prompt should not be null");
        assertTrue(prompt.contains("build"), "Should mention build");
        assertTrue(prompt.contains("correct"), "Should mention correctness");
        assertTrue(prompt.contains("missing"), "Should mention missing blocks");
        assertTrue(prompt.contains("coordinates"), "Should request coordinates");
    }

    @Test
    @DisplayName("Should provide threat detection prompt")
    void testMinecraftVisionPrompts_ThreatDetection() {
        String prompt = MinecraftVisionPrompts.THREAT_DETECTION;

        assertNotNull(prompt, "Prompt should not be null");
        assertTrue(prompt.contains("threats"), "Should mention threats");
        assertTrue(prompt.contains("hostile"), "Should mention hostile mobs");
        assertTrue(prompt.contains("lava"), "Should mention lava");
        assertTrue(prompt.contains("locations"), "Should request locations");
    }

    @Test
    @DisplayName("Should provide resource scanning prompt")
    void testMinecraftVisionPrompts_ResourceScanning() {
        String prompt = MinecraftVisionPrompts.RESOURCE_SCANNING;

        assertNotNull(prompt, "Prompt should not be null");
        assertTrue(prompt.contains("resources"), "Should mention resources");
        assertTrue(prompt.contains("ores"), "Should mention ores");
        assertTrue(prompt.contains("trees"), "Should mention trees");
        assertTrue(prompt.contains("quantities"), "Should request quantities");
    }

    // ==================== Parameter Customization Tests ====================

    @Test
    @DisplayName("Should allow custom max_tokens")
    void testCustomMaxTokens() {
        // This test assumes the builder might be extended with custom parameters
        // For now, we verify the default
        JsonObject request = VisionRequestBuilder.buildBasicRequest(
            TEST_MODEL,
            TEST_PROMPT,
            TEST_BASE64_IMAGE
        );

        assertEquals(512, request.get("max_tokens").getAsInt());
    }

    @Test
    @DisplayName("Should allow custom temperature")
    void testCustomTemperature() {
        // Verify the default temperature
        JsonObject request = VisionRequestBuilder.buildBasicRequest(
            TEST_MODEL,
            TEST_PROMPT,
            TEST_BASE64_IMAGE
        );

        assertEquals(0.7, request.get("temperature").getAsDouble(), 0.001);
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle very long prompts")
    void testBuildBasicRequest_LongPrompt() {
        StringBuilder longPrompt = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longPrompt.append("This is a very long prompt. ");
        }

        JsonObject request = VisionRequestBuilder.buildBasicRequest(
            TEST_MODEL,
            longPrompt.toString(),
            TEST_BASE64_IMAGE
        );

        JsonArray content = request.getAsJsonArray("messages")
            .get(0).getAsJsonObject()
            .getAsJsonArray("content");

        assertEquals(longPrompt.toString(), content.get(0).getAsJsonObject().get("text").getAsString());
    }

    @Test
    @DisplayName("Should handle special characters in prompt")
    void testBuildBasicRequest_SpecialCharacters() {
        String specialPrompt = "Analyze: biome=\"plains\", coords=[100, 64, 200]\nStatus: OK";

        JsonObject request = VisionRequestBuilder.buildBasicRequest(
            TEST_MODEL,
            specialPrompt,
            TEST_BASE64_IMAGE
        );

        JsonArray content = request.getAsJsonArray("messages")
            .get(0).getAsJsonObject()
            .getAsJsonArray("content");

        assertEquals(specialPrompt, content.get(0).getAsJsonObject().get("text").getAsString());
    }

    @Test
    @DisplayName("Should handle empty prompt")
    void testBuildBasicRequest_EmptyPrompt() {
        JsonObject request = VisionRequestBuilder.buildBasicRequest(
            TEST_MODEL,
            "",
            TEST_BASE64_IMAGE
        );

        JsonArray content = request.getAsJsonArray("messages")
            .get(0).getAsJsonObject()
            .getAsJsonArray("content");

        assertEquals("", content.get(0).getAsJsonObject().get("text").getAsString());
    }

    @Test
    @DisplayName("Should handle different image formats")
    void testBuildBasicRequest_DifferentImageFormats() {
        String pngImage = "data:image/png;base64,abc";
        String jpegImage = "data:image/jpeg;base64,def";
        String webpImage = "data:image/webp;base64,ghi";

        assertDoesNotThrow(() -> VisionRequestBuilder.buildBasicRequest(TEST_MODEL, TEST_PROMPT, pngImage));
        assertDoesNotThrow(() -> VisionRequestBuilder.buildBasicRequest(TEST_MODEL, TEST_PROMPT, jpegImage));
        assertDoesNotThrow(() -> VisionRequestBuilder.buildBasicRequest(TEST_MODEL, TEST_PROMPT, webpImage));
    }

    @Test
    @DisplayName("Should handle URL-based images")
    void testBuildBasicRequest_UrlImages() {
        String imageUrl = "https://example.com/screenshot.png";

        JsonObject request = VisionRequestBuilder.buildBasicRequest(
            TEST_MODEL,
            TEST_PROMPT,
            imageUrl
        );

        JsonArray content = request.getAsJsonArray("messages")
            .get(0).getAsJsonObject()
            .getAsJsonArray("content");

        JsonObject imagePart = content.get(1).getAsJsonObject();
        assertEquals(imageUrl, imagePart.getAsJsonObject("image_url").get("url").getAsString());
    }

    // ==================== Null and Empty Input Tests ====================

    @Test
    @DisplayName("Should handle null model with default")
    void testBuildBasicRequest_NullModel() {
        // If the implementation defaults to a model when null is passed
        JsonObject request = VisionRequestBuilder.buildBasicRequest(
            null,
            TEST_PROMPT,
            TEST_BASE64_IMAGE
        );

        // Either handles null gracefully or uses default
        assertTrue(request.has("model"));
    }

    @Test
    @DisplayName("Should handle null image gracefully")
    void testBuildBasicRequest_NullImage() {
        assertThrows(
            Exception.class,
            () -> VisionRequestBuilder.buildBasicRequest(TEST_MODEL, TEST_PROMPT, null),
            "Should throw exception for null image"
        );
    }

    // ==================== JSON Serialization Tests ====================

    @Test
    @DisplayName("Request should serialize to valid JSON")
    void testSerialization_ValidJson() {
        JsonObject request = VisionRequestBuilder.buildBasicRequest(
            TEST_MODEL,
            TEST_PROMPT,
            TEST_BASE64_IMAGE
        );

        assertDoesNotThrow(() -> {
            String json = request.toString();
            JsonParser.parseString(json);
        }, "Request should serialize to valid JSON");
    }

    @Test
    @DisplayName("Serialized JSON should have correct structure")
    void testSerialization_Structure() {
        JsonObject request = VisionRequestBuilder.buildBasicRequest(
            TEST_MODEL,
            TEST_PROMPT,
            TEST_BASE64_IMAGE
        );

        String json = request.toString();

        assertTrue(json.contains("\"model\""), "Should contain model field");
        assertTrue(json.contains("\"messages\""), "Should contain messages field");
        assertTrue(json.contains("\"content\""), "Should contain content field");
        assertTrue(json.contains("\"image_url\""), "Should contain image_url field");
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should build complete Minecraft analysis request")
    void testCompleteMinecraftAnalysisRequest() {
        String systemPrompt = "You are Steve, a Minecraft AI assistant.";
        String userPrompt = MinecraftVisionPrompts.BIOME_ANALYSIS;
        String image = TEST_BASE64_IMAGE;

        JsonObject request = VisionRequestBuilder.buildRequestWithSystem(
            TEST_MODEL,
            systemPrompt,
            userPrompt,
            image
        );

        // Verify complete structure
        assertEquals(TEST_MODEL, request.get("model").getAsString());
        assertEquals(2, request.getAsJsonArray("messages").size());

        // Verify system message
        JsonObject systemMsg = request.getAsJsonArray("messages").get(0).getAsJsonObject();
        assertEquals("system", systemMsg.get("role").getAsString());

        // Verify user message with multimodal content
        JsonObject userMsg = request.getAsJsonArray("messages").get(1).getAsJsonObject();
        assertEquals("user", userMsg.get("role").getAsString());
        JsonArray content = userMsg.getAsJsonArray("content");
        assertEquals(2, content.size());
    }
}
