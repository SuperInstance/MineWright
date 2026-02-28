# Vision Model Integration Guide for Minecraft Modding

**Date:** 2026-02-27
**Purpose:** Practical guide for integrating vision models (SmolVLM, GLM-4.6v) with Minecraft Forge 1.20.1 for AI assistant use

---

## Table of Contents

1. [Screenshot Capture in Minecraft Forge 1.20.1](#1-screenshot-capture-in-minecraft-forge-1201)
2. [Converting Frame Buffer to Base64](#2-converting-minecraft-frame-buffer-to-base64)
3. [OpenAI-Compatible Vision API Format](#3-openai-compatible-vision-api-format)
4. [Example Request Body for Vision + Text](#4-example-request-body-for-vision--text)
5. [Parsing Vision Model Responses](#5-parsing-vision-model-responses)
6. [Performance Considerations](#6-performance-considerations)
7. [Local vs Cloud Vision](#7-local-vs-cloud-vision)

---

## 1. Screenshot Capture in Minecraft Forge 1.20.1

### 1.1 Using Minecraft's Built-in Screenshot Class

Minecraft Forge 1.20.1 provides the `net.minecraft.util.Screenshot` class for capturing game screenshots.

```java
package com.steve.vision;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Screenshot;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Captures screenshots from Minecraft's main framebuffer.
 */
public class ScreenshotCapture {

    private static final Minecraft MC = Minecraft.getInstance();

    /**
     * Captures a screenshot of the current game view.
     *
     * @param fileName The filename to save (without extension)
     * @return Path to the saved screenshot
     */
    public static Path captureScreenshot(String fileName) {
        // Get the main render target (framebuffer)
        RenderTarget mainTarget = MC.getMainRenderTarget();

        // Create screenshots directory
        Path screenshotDir = Paths.get("screenshots", "vision");
        screenshotDir.toFile().mkdirs();

        Path screenshotPath = screenshotDir.resolve(fileName + ".png");

        // Use Minecraft's built-in screenshot functionality
        Screenshot.grab(
            MC.gameRenderer,
            mainTarget.width,
            mainTarget.height,
            screenshotPath,
            (message) -> {
                // Optional: Log screenshot capture
                MC.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Screenshot captured: " + fileName),
                    false
                );
            }
        );

        return screenshotPath;
    }

    /**
     * Captures a screenshot with timestamp in filename.
     */
    public static Path captureTimestampedScreenshot() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return captureScreenshot("vision_" + timestamp);
    }

    /**
     * Gets framebuffer dimensions.
     */
    public static Dimension getFramebufferSize() {
        RenderTarget mainTarget = MC.getMainRenderTarget();
        return new Dimension(mainTarget.width, mainTarget.height);
    }

    /**
     * Simple dimension record.
     */
    public record Dimension(int width, int height) {}
}
```

### 1.2 Direct Framebuffer Access

For more control over the screenshot capture (e.g., excluding UI elements):

```java
package com.steve.vision;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Advanced screenshot capture with direct framebuffer access.
 */
public class AdvancedScreenshotCapture {

    private static final Minecraft MC = Minecraft.getInstance();

    /**
     * Captures a screenshot without UI elements (HUD).
     */
    public static Path captureCleanScreenshot(String fileName) throws Exception {
        RenderTarget mainTarget = MC.getMainRenderTarget();
        int width = mainTarget.width;
        int height = mainTarget.height;

        // Create buffer for pixel data
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

        // Bind framebuffer and read pixels
        mainTarget.bindWrite(true);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Convert to BufferedImage
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Convert RGBA to ARGB and flip vertically (OpenGL origin is bottom-left)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = ((height - 1 - y) * width + x) * 4;
                int r = buffer.get(index) & 0xFF;
                int g = buffer.get(index + 1) & 0xFF;
                int b = buffer.get(index + 2) & 0xFF;
                int a = buffer.get(index + 3) & 0xFF;
                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        // Save to file
        Path screenshotPath = Paths.get("screenshots", "vision", fileName + ".png");
        Files.createDirectories(screenshotPath.getParent());
        ImageIO.write(image, "PNG", screenshotPath.toFile());

        return screenshotPath;
    }

    /**
     * Captures a resized screenshot (smaller for faster transmission).
     */
    public static Path captureResizedScreenshot(String fileName, int maxWidth, int maxHeight) throws Exception {
        Path fullScreenshot = captureCleanScreenshot(fileName + "_full");

        // Load and resize
        BufferedImage original = ImageIO.read(fullScreenshot.toFile());

        // Calculate scaled dimensions maintaining aspect ratio
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        double scale = Math.min(
            (double) maxWidth / originalWidth,
            (double) maxHeight / originalHeight
        );

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // Create resized image
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                          java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        // Save resized version
        Path resizedPath = Paths.get("screenshots", "vision", fileName + ".png");
        ImageIO.write(resized, "PNG", resizedPath.toFile());

        // Delete full version
        Files.delete(fullScreenshot);

        return resizedPath;
    }
}
```

### 1.3 Scheduling Screenshots in the Game Loop

```java
package com.steve.vision;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Manages screenshot capture timing in the game loop.
 */
@Mod.EventBusSubscriber(modid = "steve", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScreenshotScheduler {

    private static int screenshotDelay = 0;
    private static Runnable pendingScreenshotAction = null;

    /**
     * Schedule a screenshot to be captured after the specified delay.
     *
     * @param delayTicks Number of game ticks to wait (20 ticks = 1 second)
     * @param action Callback to execute with the screenshot path
     */
    public static void scheduleScreenshot(int delayTicks, Runnable action) {
        screenshotDelay = delayTicks;
        pendingScreenshotAction = action;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (screenshotDelay > 0) {
            screenshotDelay--;
            if (screenshotDelay == 0 && pendingScreenshotAction != null) {
                // Capture on the main thread
                try {
                    Path screenshot = ScreenshotCapture.captureTimestampedScreenshot();
                    pendingScreenshotAction.run();
                } catch (Exception e) {
                    // Handle error
                    e.printStackTrace();
                }
                pendingScreenshotAction = null;
            }
        }
    }
}
```

---

## 2. Converting Minecraft Frame Buffer to Base64

### 2.1 Simple Base64 Encoding

```java
package com.steve.vision;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Converts image files to base64 encoded data URLs.
 */
public class ImageEncoder {

    /**
     * Encodes an image file to a base64 data URL.
     *
     * @param imagePath Path to the image file
     * @return Base64 data URL (e.g., "data:image/png;base64,iVBORw0K...")
     * @throws Exception If file cannot be read
     */
    public static String encodeToBase64(Path imagePath) throws Exception {
        byte[] imageBytes = Files.readAllBytes(imagePath);
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = detectMimeType(imagePath);
        return "data:" + mimeType + ";base64," + base64;
    }

    /**
     * Detects MIME type from file extension.
     */
    private static String detectMimeType(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".webp")) return "image/webp";
        if (filename.endsWith(".gif")) return "image/gif";
        return "image/png"; // Default
    }

    /**
     * Encodes an image file to base64 without the data URL prefix.
     * Use this if your API expects raw base64.
     */
    public static String encodeToRawBase64(Path imagePath) throws Exception {
        byte[] imageBytes = Files.readAllBytes(imagePath);
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
```

### 2.2 Streaming Base64 Encoding (For Large Images)

```java
package com.steve.vision;

import java.io.*;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Efficient base64 encoding for large images using streaming.
 */
public class StreamingImageEncoder {

    private static final int BUFFER_SIZE = 8192;

    /**
     * Encodes an image using streaming to reduce memory overhead.
     *
     * @param imagePath Path to the image file
     * @param mimeType MIME type of the image
     * @param outputStream Output stream to write the encoded data
     * @throws Exception If encoding fails
     */
    public static void encodeStreaming(Path imagePath, String mimeType, OutputStream outputStream) throws Exception {
        try (InputStream inputStream = Files.newInputStream(imagePath);
             OutputStream base64Stream = Base64.getEncoder().wrap(outputStream)) {

            // Write data URL prefix
            String prefix = "data:" + mimeType + ";base64,";
            outputStream.write(prefix.getBytes());

            // Stream the encoded image data
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                base64Stream.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * Creates a string builder with streaming base64 encoding.
     */
    public static String encodeToString(Path imagePath, String mimeType) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        encodeStreaming(imagePath, mimeType, outputStream);
        return outputStream.toString("UTF-8");
    }
}
```

### 2.3 Direct Framebuffer to Base64 (No Intermediate File)

```java
package com.steve.vision;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.opengl.GL11;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/**
 * Captures screenshot directly to base64 without saving to disk.
 */
public class DirectBase64Capture {

    private static final Minecraft MC = Minecraft.getInstance();

    /**
     * Captures a screenshot and returns it as a base64 data URL.
     *
     * @param maxWidth Maximum width (scale down if larger, 0 for no scaling)
     * @param maxHeight Maximum height (scale down if larger, 0 for no scaling)
     * @return Base64 data URL of the screenshot
     * @throws Exception If capture fails
     */
    public static String captureToBase64(int maxWidth, int maxHeight) throws Exception {
        RenderTarget mainTarget = MC.getMainRenderTarget();
        int width = mainTarget.width;
        int height = mainTarget.height;

        // Read pixels from framebuffer
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        mainTarget.bindWrite(true);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Convert to BufferedImage
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = ((height - 1 - y) * width + x) * 4;
                int r = buffer.get(index) & 0xFF;
                int g = buffer.get(index + 1) & 0xFF;
                int b = buffer.get(index + 2) & 0xFF;
                int a = buffer.get(index + 3) & 0xFF;
                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        // Resize if needed
        if (maxWidth > 0 && maxHeight > 0 && (width > maxWidth || height > maxHeight)) {
            image = resizeImage(image, maxWidth, maxHeight);
        }

        // Encode to PNG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();

        // Encode to base64
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64;
    }

    /**
     * Resizes an image maintaining aspect ratio.
     */
    private static BufferedImage resizeImage(BufferedImage original, int maxWidth, int maxHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        double scale = Math.min(
            (double) maxWidth / originalWidth,
            (double) maxHeight / originalHeight
        );

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                          java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }

    /**
     * Quick capture with default resolution (1280x720 max).
     */
    public static String quickCapture() throws Exception {
        return captureToBase64(1280, 720);
    }
}
```

---

## 3. OpenAI-Compatible Vision API Format

### 3.1 Standard Multimodal Message Format

Both local models (SmolVLM via vLLM) and cloud APIs (GLM-4.6v, OpenAI GPT-4V) use the **OpenAI-compatible multimodal format**:

```json
{
  "model": "model-name",
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "Your text prompt here"
        },
        {
          "type": "image_url",
          "image_url": {
            "url": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
            "detail": "auto"
          }
        }
      ]
    }
  ],
  "max_tokens": 512,
  "temperature": 0.7
}
```

### 3.2 Content Array Format

The `content` field is an **array** of content parts, not a string:

| Part Type | Fields | Description |
|-----------|--------|-------------|
| `text` | `type: "text"`, `text: string` | Text prompt |
| `image_url` | `type: "image_url"`, `image_url: {url, detail}` | Image (base64 or URL) |

### 3.3 Image URL Format

**Base64 Data URL (Recommended):**
```
data:<mime-type>;base64,<base64-data>
```

**Supported MIME Types:**
- `image/png`
- `image/jpeg`
- `image/webp`
- `image/gif`

**HTTP/HTTPS URL:**
```
https://example.com/screenshot.png
```

### 3.4 Detail Parameter

Controls how the model processes the image:

| Value | Description | Token Cost |
|-------|-------------|------------|
| `auto` | Model decides (default) | Variable |
| `low` | Low resolution, faster | ~85 tokens |
| `high` | High resolution, detailed | ~765 tokens |

---

## 4. Example Request Body for Vision + Text

### 4.1 Basic Vision Request

```java
package com.steve.vision;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * Builds vision API request bodies.
 */
public class VisionRequestBuilder {

    /**
     * Creates a basic vision request with image and text.
     */
    public static JsonObject buildBasicRequest(String model, String prompt, String base64Image) {
        JsonObject request = new JsonObject();

        // Model and generation parameters
        request.addProperty("model", model);
        request.addProperty("max_tokens", 512);
        request.addProperty("temperature", 0.7);

        // Messages array
        JsonArray messages = new JsonArray();

        // User message with multimodal content
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");

        // Content array with text and image
        JsonArray content = new JsonArray();

        // Text part
        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", prompt);
        content.add(textPart);

        // Image part
        JsonObject imagePart = new JsonObject();
        imagePart.addProperty("type", "image_url");

        JsonObject imageUrl = new JsonObject();
        imageUrl.addProperty("url", base64Image);
        imageUrl.addProperty("detail", "auto");

        imagePart.add("image_url", imageUrl);
        content.add(imagePart);

        userMessage.add("content", content);
        messages.add(userMessage);

        request.add("messages", messages);

        return request;
    }
}
```

### 4.2 Multi-Image Comparison Request

```java
/**
 * Creates a request for comparing multiple images.
 */
public static JsonObject buildComparisonRequest(String model, String prompt, String... base64Images) {
    JsonObject request = new JsonObject();
    request.addProperty("model", model);
    request.addProperty("max_tokens", 512);
    request.addProperty("temperature", 0.7);

    JsonArray messages = new JsonArray();

    JsonObject userMessage = new JsonObject();
    userMessage.addProperty("role", "user");

    JsonArray content = new JsonArray();

    // Add text prompt
    JsonObject textPart = new JsonObject();
    textPart.addProperty("type", "text");
    textPart.addProperty("text", prompt);
    content.add(textPart);

    // Add all images
    for (String base64Image : base64Images) {
        JsonObject imagePart = new JsonObject();
        imagePart.addProperty("type", "image_url");

        JsonObject imageUrl = new JsonObject();
        imageUrl.addProperty("url", base64Image);

        imagePart.add("image_url", imageUrl);
        content.add(imagePart);
    }

    userMessage.add("content", content);
    messages.add(userMessage);

    request.add("messages", messages);

    return request;
}
```

### 4.3 Request with System Prompt

```java
/**
 * Creates a vision request with a system prompt for context.
 */
public static JsonObject buildRequestWithSystem(
    String model,
    String systemPrompt,
    String userPrompt,
    String base64Image
) {
    JsonObject request = new JsonObject();
    request.addProperty("model", model);
    request.addProperty("max_tokens", 512);
    request.addProperty("temperature", 0.7);

    JsonArray messages = new JsonArray();

    // System message (text only)
    if (systemPrompt != null && !systemPrompt.isEmpty()) {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);
    }

    // User message with multimodal content
    JsonObject userMessage = new JsonObject();
    userMessage.addProperty("role", "user");

    JsonArray content = new JsonArray();

    JsonObject textPart = new JsonObject();
    textPart.addProperty("type", "text");
    textPart.addProperty("text", userPrompt);
    content.add(textPart);

    JsonObject imagePart = new JsonObject();
    imagePart.addProperty("type", "image_url");

    JsonObject imageUrl = new JsonObject();
    imageUrl.addProperty("url", base64Image);

    imagePart.add("image_url", imageUrl);
    content.add(imagePart);

    userMessage.add("content", content);
    messages.add(userMessage);

    request.add("messages", messages);

    return request;
}
```

### 4.4 Example Usage for Minecraft Analysis

```java
/**
 * Example prompts for Minecraft-specific analysis.
 */
public class MinecraftVisionPrompts {

    public static final String BIOME_ANALYSIS = """
        Analyze this Minecraft screenshot and identify:
        1. The biome type
        2. Visible terrain features
        3. Available resources (trees, ore, water)
        4. Potential threats or dangers
        5. Suggested next actions for exploration

        Format your response as a JSON object with these fields.
        """;

    public static final String OCR_READING = """
        Read all text visible in this Minecraft screenshot.
        Include text from:
        - Signs
        - Chat messages
        - Scoreboard/objectives
        - Item tooltips if visible

        Transcribe the text exactly as written, preserving formatting.
        """;

    public static final String BUILD_VERIFICATION = """
        Compare this Minecraft build with what should be there.
        Identify:
        1. What blocks are correct
        2. What blocks are missing
        3. What blocks are incorrect
        4. Any structural issues

        Provide specific coordinates for issues if possible.
        """;

    public static final String THREAT_DETECTION = """
        Scan this Minecraft screenshot for dangers:
        - Hostile mobs (zombies, skeletons, creepers, etc.)
        - Environmental hazards (lava, cliffs, deep water)
        - Mob spawners
        - Monster rooms

        List all threats found with their approximate locations.
        """;

    public static final String RESOURCE_SCANNING = """
        Identify all visible resources in this Minecraft screenshot:
        - Ores (coal, iron, gold, diamond, etc.)
        - Trees and wood types
        - Food sources (animals, crops)
        - Water sources
        - Chests or containers

        Estimate quantities and note accessibility.
        """;
}
```

---

## 5. Parsing Vision Model Responses

### 5.1 Standard Response Format

Both local and cloud models return responses in the same format:

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "model-name",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "The analysis result..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 850,
    "completion_tokens": 150,
    "total_tokens": 1000
  }
}
```

### 5.2 Response Parser

```java
package com.steve.vision;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

/**
 * Parses vision model API responses.
 */
public class VisionResponseParser {

    /**
     * Extracts the content text from a vision API response.
     *
     * @param jsonResponse The raw JSON response string
     * @return The assistant's content, or null if parsing fails
     */
    public static String extractContent(String jsonResponse) {
        try {
            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();

            // Check for choices array
            if (response.has("choices")) {
                JsonArray choices = response.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();

                    // Check for message object
                    if (firstChoice.has("message")) {
                        JsonObject message = firstChoice.getAsJsonObject("message");

                        // Extract content
                        if (message.has("content")) {
                            return message.get("content").getAsString();
                        }
                    }
                }
            }

            // Check for error object
            if (response.has("error")) {
                JsonObject error = response.getAsJsonObject("error");
                String errorMessage = error.has("message")
                    ? error.get("message").getAsString()
                    : "Unknown error";
                throw new RuntimeException("API Error: " + errorMessage);
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse vision response: " + e.getMessage(), e);
        }
    }

    /**
     * Parses usage statistics from the response.
     */
    public static UsageStats extractUsageStats(String jsonResponse) {
        try {
            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (response.has("usage")) {
                JsonObject usage = response.getAsJsonObject("usage");
                return new UsageStats(
                    usage.get("prompt_tokens").getAsInt(),
                    usage.get("completion_tokens").getAsInt(),
                    usage.get("total_tokens").getAsInt()
                );
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if the response was truncated.
     */
    public static boolean wasTruncated(String jsonResponse) {
        try {
            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray choices = response.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                String finishReason = firstChoice.get("finish_reason").getAsString();
                return "length".equals(finishReason);
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    /**
     * Usage statistics record.
     */
    public record UsageStats(int promptTokens, int completionTokens, int totalTokens) {}
}
```

### 5.3 Structured Response Parser (for JSON outputs)

```java
package com.steve.vision;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Map;

/**
 * Parses structured JSON responses from vision models.
 */
public class StructuredVisionParser {

    private static final Gson GSON = new Gson();

    /**
     * Parses a JSON object response from the model.
     * Use this when prompting the model to return JSON.
     */
    public static Map<String, Object> parseJsonResponse(String modelResponse) {
        try {
            // Try to extract JSON from markdown code blocks
            String cleaned = modelResponse;

            // Remove ```json and ``` markers
            if (cleaned.contains("```json")) {
                cleaned = cleaned.substring(cleaned.indexOf("```json") + 7);
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
            } else if (cleaned.contains("```")) {
                cleaned = cleaned.substring(cleaned.indexOf("```") + 3);
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
            }

            // Parse as JSON
            return GSON.fromJson(cleaned.trim(), Map.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse structured response: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a biome analysis response.
     */
    public static BiomeAnalysis parseBiomeAnalysis(String modelResponse) {
        Map<String, Object> data = parseJsonResponse(modelResponse);
        return new BiomeAnalysis(
            (String) data.get("biome"),
            (String) data.get("terrainFeatures"),
            (String) data.get("resources"),
            (String) data.get("threats"),
            (String) data.get("suggestedActions")
        );
    }

    /**
     * Biome analysis record.
     */
    public record BiomeAnalysis(
        String biome,
        String terrainFeatures,
        String resources,
        String threats,
        String suggestedActions
    ) {}
}
```

### 5.4 Error Handling

```java
package com.steve.vision;

/**
 * Handles vision API errors with helpful recovery suggestions.
 */
public class VisionErrorHandler {

    public static VisionError classifyError(String jsonResponse, int httpStatus) {
        try {
            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (response.has("error")) {
                JsonObject error = response.getAsJsonObject("error");
                String type = error.has("type") ? error.get("type").getAsString() : "";
                String message = error.get("message").getAsString();

                return switch (type) {
                    case "invalid_request_error" -> new VisionError(
                        VisionErrorType.INVALID_REQUEST,
                        message,
                        "Check your request format and parameters."
                    );
                    case "authentication_error" -> new VisionError(
                        VisionErrorType.AUTHENTICATION,
                        message,
                        "Check your API key configuration."
                    );
                    case "rate_limit_error" -> new VisionError(
                        VisionErrorType.RATE_LIMIT,
                        message,
                        "Wait before making another request, or upgrade your plan."
                    );
                    case "server_error" -> new VisionError(
                        VisionErrorType.SERVER,
                        message,
                        "The API is experiencing issues. Try again later."
                    );
                    default -> new VisionError(
                        VisionErrorType.UNKNOWN,
                        message,
                        "An unknown error occurred."
                    );
                };
            }
        } catch (Exception e) {
            // Ignore parse errors
        }

        // Classify by HTTP status
        return switch (httpStatus) {
            case 401, 403 -> new VisionError(
                VisionErrorType.AUTHENTICATION,
                "Authentication failed",
                "Check your API key configuration."
            );
            case 429 -> new VisionError(
                VisionErrorType.RATE_LIMIT,
                "Rate limit exceeded",
                "Wait before making another request."
            );
            case 500, 502, 503 -> new VisionError(
                VisionErrorType.SERVER,
                "Server error",
                "The API is experiencing issues. Try again later."
            );
            default -> new VisionError(
                VisionErrorType.UNKNOWN,
                "HTTP " + httpStatus,
                "Check the request and try again."
            );
        };
    }

    public enum VisionErrorType {
        INVALID_REQUEST,
        AUTHENTICATION,
        RATE_LIMIT,
        SERVER,
        UNKNOWN
    }

    public record VisionError(
        VisionErrorType type,
        String message,
        String suggestion
    ) {}
}
```

---

## 6. Performance Considerations

### 6.1 Image Resolution Recommendations

| Use Case | Recommended Resolution | File Size | Notes |
|----------|------------------------|-----------|-------|
| **OCR (Text Reading)** | 1920x1080 or higher | ~500KB | Higher resolution needed for small text |
| **Biome Identification** | 640x480 | ~100KB | Lower resolution sufficient |
| **Resource Scanning** | 1280x720 | ~200KB | Balance detail and speed |
| **Threat Detection** | 1280x720 | ~200KB | Need to see mobs clearly |
| **Build Verification** | 1920x1080 | ~500KB | Need detail for accuracy |

### 6.2 Image Compression

```java
package com.steve.vision;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Image compression utilities for optimizing vision API requests.
 */
public class ImageCompressor {

    /**
     * Compresses an image to JPEG with specified quality.
     *
     * @param image The image to compress
     * @param quality Quality (0.0 to 1.0)
     * @return Compressed image bytes
     */
    public static byte[] compressToJpeg(BufferedImage image, float quality) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        writer.setOutput(new MemoryCacheImageOutputStream(baos));
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();

        return baos.toByteArray();
    }

    /**
     * Compresses a PNG image.
     * PNG is lossless but can be optimized.
     */
    public static byte[] compressToPng(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Recommended quality settings for different use cases.
     */
    public static final float OCR_QUALITY = 0.95f;      // High quality for text
    public static final float STANDARD_QUALITY = 0.85f; // Good balance
    public static final float FAST_QUALITY = 0.7f;      // Speed over quality
}
```

### 6.3 Token Cost Estimation

Vision models consume tokens based on image detail:

| Detail Level | Token Cost | When to Use |
|--------------|------------|-------------|
| `low` | ~85 tokens | Quick analysis, large images |
| `auto` | ~85-765 tokens | General purpose (default) |
| `high` | ~765 tokens | Detailed analysis, OCR |

**Example with 1920x1080 image:**
- Low detail: ~85 prompt tokens
- Auto detail: ~300-500 prompt tokens (estimated)
- High detail: ~765 prompt tokens

### 6.4 Latency Benchmarks

Based on research data for **RTX 4060**:

| Model | Single Image | Multi-Image (2) | Video (50 frames) |
|-------|--------------|-----------------|-------------------|
| **SmolVLM2-256M (Local)** | 0.5-1.0s | 1.0-2.0s | 5-10s |
| **SmolVLM2-2.2B (Local)** | 1.0-2.0s | 2.0-4.0s | 10-20s |
| **GLM-4.6v (Cloud)** | 2-4s | 4-8s | Not recommended |
| **GPT-4V (Cloud)** | 4-8s | 8-16s | Not recommended |

**Key Finding:** Local models are **2-3x faster** than cloud APIs for vision tasks.

### 6.5 Optimization Strategies

```java
package com.steve.vision;

/**
 * Optimization strategies for vision API performance.
 */
public class VisionOptimization {

    /**
     * Recommended batch size for multiple images.
     */
    public static final int MAX_IMAGES_PER_REQUEST = 4;

    /**
     * Maximum recommended resolution for different scenarios.
     */
    public static final Resolution LOW_RES = new Resolution(640, 480);
    public static final Resolution MEDIUM_RES = new Resolution(1280, 720);
    public static final Resolution HIGH_RES = new Resolution(1920, 1080);

    public record Resolution(int width, int height) {}

    /**
     * Suggests optimal resolution based on task type.
     */
    public static Resolution suggestResolution(VisionTask task) {
        return switch (task) {
            case OCR -> HIGH_RES;           // Need detail for text
            case BIOME_IDENTIFICATION -> LOW_RES;    // Don't need much detail
            case RESOURCE_SCANNING -> MEDIUM_RES;    // Balance
            case THREAT_DETECTION -> MEDIUM_RES;     // Need to see mobs
            case BUILD_VERIFICATION -> HIGH_RES;     // Need accuracy
            case GENERAL_ANALYSIS -> MEDIUM_RES;     // Good default
        };
    }

    public enum VisionTask {
        OCR,
        BIOME_IDENTIFICATION,
        RESOURCE_SCANNING,
        THREAT_DETECTION,
        BUILD_VERIFICATION,
        GENERAL_ANALYSIS
    }

    /**
     * Suggests detail level based on task.
     */
    public static String suggestDetailLevel(VisionTask task) {
        return switch (task) {
            case OCR, BUILD_VERIFICATION -> "high";
            case GENERAL_ANALYSIS -> "auto";
            default -> "low";
        };
    }
}
```

### 6.6 Caching Strategy

```java
package com.steve.vision;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

/**
 * Caches vision analysis results to avoid redundant API calls.
 */
public class VisionCache {

    private static final Map<String, CachedResult> cache = new ConcurrentHashMap<>();
    private static final long DEFAULT_TTL_SECONDS = 300; // 5 minutes

    /**
     * Generates a cache key from image hash and prompt.
     */
    public static String generateCacheKey(String imageBase64, String prompt) {
        // Simple hash of first 100 chars of image + prompt
        String imageSignature = imageBase64.substring(0, Math.min(100, imageBase64.length()));
        return (imageSignature + prompt).hashCode() + "";
    }

    /**
     * Gets a cached result if available and not expired.
     */
    public static String get(String imageBase64, String prompt) {
        String key = generateCacheKey(imageBase64, prompt);
        CachedResult result = cache.get(key);

        if (result != null && !result.isExpired()) {
            return result.response();
        }

        return null;
    }

    /**
     * Puts a result in the cache.
     */
    public static void put(String imageBase64, String prompt, String response) {
        String key = generateCacheKey(imageBase64, prompt);
        cache.put(key, new CachedResult(response, Instant.now().getEpochSecond()));
    }

    /**
     * Clears expired entries from the cache.
     */
    public static void cleanup() {
        long now = Instant.now().getEpochSecond();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private record CachedResult(
        String response,
        long timestamp
    ) {
        boolean isExpired() {
            return isExpired(Instant.now().getEpochSecond());
        }

        boolean isExpired(long currentTime) {
            return (currentTime - timestamp) > DEFAULT_TTL_SECONDS;
        }
    }
}
```

---

## 7. Local vs Cloud Vision

### 7.1 Comparison Table

| Factor | Local (SmolVLM/GLM-4.6v) | Cloud (GLM-4.6v/GPT-4V) |
|--------|------------------------|-------------------------|
| **Latency** | 0.5-2s | 2-8s |
| **Cost** | Free (after hardware) | $0.01-0.03 per image |
| **Privacy** | 100% local | Data sent to API |
| **Quality** | Good (72.9% OCRBench) | Excellent |
| **Reliability** | Depends on hardware | High (99.9% uptime) |
| **Setup** | Requires GPU/VPU | Just API key |
| **Scalability** | Limited by hardware | Unlimited |
| **Offline** | Works offline | Requires internet |

### 7.2 When to Use Local Vision

**Use Local Vision When:**

1. **Real-time Interaction Required**
   - AI assistant needs to respond quickly
   - User waiting for response
   - Game loop integration

2. **Privacy Concerns**
   - Sensitive information on screen
   - Don't want to leak game state
   - Offline environments

3. **High Volume**
   - Frequent screenshot analysis
   - Many agents using vision
   - Cost would be prohibitive

4. **Consistent Latency**
   - Need predictable performance
   - Network unreliable
   - Competitive gaming

5. **OCR Tasks**
   - SmolVLM has excellent OCR (72.9% OCRBench)
   - GLM-4.6v supports OCR
   - Text reading is straightforward

### 7.3 When to Use Cloud Vision

**Use Cloud Vision When:**

1. **Complex Visual Reasoning**
   - Detailed scene understanding
   - Complex spatial relationships
   - Multi-image comparison

2. **Better Quality Required**
   - Critical tasks where accuracy matters
   - Edge cases local model struggles with
   - Fine detail analysis

3. **No GPU Available**
   - CPU-only system
   - Low-end hardware
   - Quick testing

4. **Low Volume**
   - Occasional vision tasks
   - Prototyping and development
   - One-off analyses

5. **Fallback**
   - Local model unavailable
   - Error recovery
   - Redundancy

### 7.4 Hybrid Strategy (Recommended)

```java
package com.steve.vision;

import com.steve.llm.LocalLLMClient;
import com.steve.llm.GLMCascadeRouter;

/**
 * Routes vision requests to local or cloud based on task complexity.
 */
public class HybridVisionRouter {

    private final LocalLLMClient localClient;
    private final GLMCascadeRouter cloudRouter;

    public HybridVisionRouter() {
        this.localClient = new LocalLLMClient();
        this.cloudRouter = new GLMCascadeRouter();
    }

    /**
     * Routes vision request to optimal provider.
     */
    public VisionResult analyze(String prompt, String base64Image) {
        VisionTask task = classifyTask(prompt);

        // Try local first for simple tasks
        if (shouldUseLocal(task)) {
            try {
                long start = System.currentTimeMillis();
                String result = localClient.analyzeScreenshot(base64Image, prompt);
                long duration = System.currentTimeMillis() - start;

                return new VisionResult(
                    result,
                    VisionProvider.LOCAL,
                    duration,
                    true
                );
            } catch (Exception e) {
                // Fall through to cloud
            }
        }

        // Use cloud for complex tasks or fallback
        try {
            long start = System.currentTimeMillis();
            String result = cloudRouter.processVisionRequest(
                "You are a Minecraft AI assistant.",
                prompt,
                base64Image
            );
            long duration = System.currentTimeMillis() - start;

            return new VisionResult(
                result,
                VisionProvider.CLOUD,
                duration,
                false
            );
        } catch (Exception e) {
            return new VisionResult(
                "Error: " + e.getMessage(),
                VisionProvider.NONE,
                0,
                false
            );
        }
    }

    /**
     * Classifies the task type.
     */
    private VisionTask classifyTask(String prompt) {
        String lower = prompt.toLowerCase();

        if (lower.contains("ocr") || lower.contains("read") || lower.contains("text")) {
            return VisionTask.OCR;
        } else if (lower.contains("biome") || lower.contains("identify")) {
            return VisionTask.BIOME_IDENTIFICATION;
        } else if (lower.contains("compare") || lower.contains("difference")) {
            return VisionTask.COMPARISON;
        } else if (lower.contains("threat") || lower.contains("danger")) {
            return VisionTask.THREAT_DETECTION;
        } else if (lower.contains("verify") || lower.contains("check build")) {
            return VisionTask.BUILD_VERIFICATION;
        } else {
            return VisionTask.GENERAL;
        }
    }

    /**
     * Determines if local model is sufficient.
     */
    private boolean shouldUseLocal(VisionTask task) {
        // Use local for OCR and simple identification
        return switch (task) {
            case OCR, BIOME_IDENTIFICATION, THREAT_DETECTION -> true;
            case BUILD_VERIFICATION, COMPARISON -> false;  // Use cloud for accuracy
            case GENERAL -> localClient.isAvailable() && localClient.isVisionSupported();
        };
    }

    public enum VisionTask {
        OCR,
        BIOME_IDENTIFICATION,
        THREAT_DETECTION,
        BUILD_VERIFICATION,
        COMPARISON,
        GENERAL
    }

    public enum VisionProvider {
        LOCAL,
        CLOUD,
        NONE
    }

    public record VisionResult(
        String content,
        VisionProvider provider,
        long latencyMs,
        boolean fromCache
    ) {}
}
```

### 7.5 Cost Analysis

**Annual Cost Comparison (assuming 1000 vision requests/day):**

| Provider | Daily Cost | Annual Cost | Hardware Cost |
|----------|------------|-------------|---------------|
| **Local SmolVLM** | $0 | $0 | ~$500 (GPU) |
| **GLM-4.6v Cloud** | ~$10-30 | ~$3,650-10,950 | $0 |
| **GPT-4V Cloud** | ~$30-50 | ~$10,950-18,250 | $0 |

**Break-even Point:** Local deployment pays for itself in **1-3 months** at high volume.

### 7.6 Performance Tips

**For Local Vision:**
1. Use quantized models (4-bit) to reduce memory
2. Lower image resolution for faster inference
3. Batch requests when possible
4. Use GPU acceleration (CUDA/Vulkan)
5. Enable NPUs if available (Ryzen AI)

**For Cloud Vision:**
1. Compress images before sending
2. Use appropriate detail level
3. Cache responses to avoid repeated calls
4. Implement exponential backoff for rate limits
5. Use streaming responses for long analyses

---

## 8. Complete Integration Example

### 8.1 Vision-Aware Action

```java
package com.steve.action.actions;

import com.steve.action.BaseAction;
import com.steve.vision.*;
import com.steve.entity.SteveEntity;
import com.steve.memory.TaskContext;
import java.nio.file.Path;

/**
 * Example action that uses vision to analyze the environment.
 */
public class VisionAnalyzeAction extends BaseAction {

    private final HybridVisionRouter visionRouter;
    private AnalysisState state = AnalysisState.CAPTURE;
    private String analysisResult;

    public VisionAnalyzeAction(SteveEntity steve, TaskContext task) {
        super(steve, task);
        this.visionRouter = new HybridVisionRouter();
    }

    @Override
    public void tick() {
        switch (state) {
            case CAPTURE -> captureScreenshot();
            case ANALYZE -> performAnalysis();
            case COMPLETE -> complete();
            case ERROR -> handleError();
        }
    }

    private void captureScreenshot() {
        try {
            // Capture screenshot
            String timestamp = String.valueOf(System.currentTimeMillis());
            Path screenshot = ScreenshotCapture.captureScreenshot("analysis_" + timestamp);

            // Encode to base64
            String base64Image = ImageEncoder.encodeToBase64(screenshot);

            // Store for analysis
            task.getMemory().set("lastScreenshot", base64Image);
            state = AnalysisState.ANALYZE;

        } catch (Exception e) {
            setErrorState("Screenshot capture failed: " + e.getMessage());
            state = AnalysisState.ERROR;
        }
    }

    private void performAnalysis() {
        String base64Image = task.getMemory().get("lastScreenshot");
        String prompt = task.getParameter("prompt",
            MinecraftVisionPrompts.BIOME_ANALYSIS);

        // Check cache first
        String cached = VisionCache.get(base64Image, prompt);
        if (cached != null) {
            analysisResult = cached;
            state = AnalysisState.COMPLETE;
            return;
        }

        // Analyze with hybrid router
        HybridVisionRouter.VisionResult result =
            visionRouter.analyze(prompt, base64Image);

        if (result.provider() == HybridVisionRouter.VisionProvider.NONE) {
            setErrorState(result.content());
            state = AnalysisState.ERROR;
            return;
        }

        analysisResult = result.content();

        // Cache the result
        VisionCache.put(base64Image, prompt, analysisResult);

        state = AnalysisState.COMPLETE;
    }

    private void complete() {
        // Store result in memory
        steve.getMemory().store("last_vision_analysis", analysisResult);

        // Log result
        info("Vision analysis complete: " + analysisResult.substring(0, 100) + "...");
    }

    @Override
    public boolean isComplete() {
        return state == AnalysisState.COMPLETE || hasError();
    }

    @Override
    public void onCancel() {
        // Cleanup if needed
    }

    private enum AnalysisState {
        CAPTURE,
        ANALYZE,
        COMPLETE,
        ERROR
    }
}
```

### 8.2 Configuration

```toml
# config/steve-common.toml

[vision]
# Enable/disable vision features
enabled = true

# Preferred provider: "local", "cloud", "hybrid"
provider = "hybrid"

# Local model settings
[vision.local]
# vLLM server URL
url = "http://localhost:8000/v1/chat/completions"
model = "HuggingFaceTB/SmolVLM2-2.2B"

# Cloud model settings
[vision.cloud]
provider = "glm-4.6v"  # or "gpt-4v"
api_key = "your-api-key"

# Performance settings
[vision.performance]
# Maximum image resolution
max_width = 1280
max_height = 720

# Image quality (0.0 to 1.0)
jpeg_quality = 0.85

# Cache TTL in seconds
cache_ttl = 300

# Maximum images per request
max_images_per_request = 4
```

---

## 9. Sources

- [SmolVLM Documentation - Hugging Face](https://hugging-face.cn/docs/transformers/model_doc/smolvlm)
- [GLM-4.6V-Flash-WEB API Guide](https://m.blog.csdn.net/weixin_32480007/article/details/157495346)
- [OpenAI Vision API Guide](https://www.openaicto.com/docs/guides/images-vision)
- [Mineshot Reforged - Minecraft Mod](https://modrinth.com/mod/mineshot-reforged)
- [Screenshot to Clipboard Mod](https://www.mcmod.cn/class/2733.html)
- [Local vs Cloud Vision Performance Comparison](https://m.blog.csdn.net/weixin_42128315/article/details/156896979)
- [Vision MCP Server](https://github.com/Loveacup/vision-mcp-server)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**For:** Steve AI - Minecraft Autonomous Agent System
