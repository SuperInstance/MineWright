# SmolVLM Vision Model Research for Minecraft AI Integration

**Date:** 2026-02-27
**Purpose:** Research document for integrating SmolVLM into Steve AI (Minecraft autonomous agent system)

---

## Executive Summary

SmolVLM is a family of lightweight vision-language models (VLMs) developed by Hugging Face, designed for edge deployment while maintaining competitive performance with much larger models. The model is ideal for Minecraft AI integration due to its minimal memory footprint, fast inference speed, and strong OCR capabilities for reading in-game text.

**Key Recommendation:** Use **SmolVLM2-2.2B** for production Minecraft AI, with **SmolVLM2-256M** for lightweight edge testing.

---

## 1. Model Architecture

### 1.1 Core Components

SmolVLM2 combines three key architectural components:

```
[Image Input]
       |
       v
+------------------+
|  SigLIP Encoder  |  (Vision: 93M - 400M parameters)
+------------------+
       |
       | 768-dimensional features
       v
+------------------+
|  Feature Mapping |  (Pixel Shuffle MLP)
|  768 -> 576 dim  |
+------------------+
       |
       | 576-dimensional features
       v
+------------------+
|   SmolLM2        |  (Language: 135M - 1.7B parameters)
|   Decoder        |
+------------------+
       |
       v
  [Text Output]
```

### 1.2 Vision Encoder (SigLIP)

| Component | SmolVLM2-256M | SmolVLM2-500M | SmolVLM2-2.2B |
|-----------|---------------|---------------|---------------|
| Vision Model | SigLIP-93M | SigLIP base | SigLIP-SO 400M |
| Architecture | ViT-based | ViT patch-16/512 | ViT-based |
| Output Dim | 768 | 768 | 768 |

**SigLIP (Sigmoid Loss for Language-Image Pre-training)** is Google's vision transformer that outperforms CLIP on zero-shot classification tasks.

### 1.3 Feature Mapping Layer (Pixel Shuffle)

The feature mapping layer is a **simple MLP with Pixel Shuffle operation** that:
- Reduces image resolution to decrease visual token count
- Maps **768 dimensions** (SigLIP output) to **576 dimensions** (SmolLM2 input)
- Improves performance for smaller VLMs compared to standard projection layers
- Reduces computational overhead while maintaining accuracy

### 1.4 Language Model (SmolLM2)

| Component | SmolVLM2-256M | SmolVLM2-500M | SmolVLM2-2.2B |
|-----------|---------------|---------------|---------------|
| Language Model | SmolLM2-135M | SmolLM2-360M | SmolLM2-1.7B |
| Embedding Dim | 576 | 576 | 576 |
| Context Length | 2048 | 2048 | 2048 |

**Key Design Decision:** SmolVLM uses **direct concatenation** of vision features with text embeddings rather than cross-attention, maximizing reuse of existing LLM components.

---

## 2. Available Model Sizes

### 2.1 Model Comparison

| Model | Parameters | VRAM (Inference) | Use Case |
|-------|------------|------------------|----------|
| **SmolVLM2-256M** | 256M | ~2GB | Edge devices, testing, mobile |
| **SmolVLM2-500M** | 500M | ~3GB | Balanced performance/consumption |
| **SmolVLM2-2.2B** | 2.2B | ~4.9GB | Production, best performance |

### 2.2 Memory Efficiency Comparison

| Model | GPU Memory | Memory vs SmolVLM |
|-------|------------|-------------------|
| **SmolVLM2-2.2B** | 4.9 GB | 1x (baseline) |
| Qwen2VL-2B | 13.7 GB | 2.8x more |
| InternVL2-2B | 10.5 GB | 2.1x more |

SmolVLM uses **2-3x less GPU memory** than comparable models.

### 2.3 Model Identifiers

For Hugging Face / vLLM:
- `HuggingFaceTB/SmolVLM-Instruct` (Original 2B model)
- `HuggingFaceTB/SmolVLM2-256M`
- `HuggingFaceTB/SmolVLM2-500M`
- `HuggingFaceTB/SmolVLM2-2.2B`

---

## 3. Vision Capabilities

### 3.1 Supported Modalities

| Capability | Support | Notes |
|------------|---------|-------|
| **Single Image** | Full | Primary use case |
| **Multi-Image** | Full | Compare multiple screenshots |
| **OCR** | Excellent (72.9% OCRBench) | Read in-game signs, books |
| **Document Parsing** | Good (80% DocVQA) | Recipe books, guides |
| **Video** | Full | 50 uniformly sampled frames |
| **Object Detection** | Good via VQA | "Where is the diamond?" |
| **Scene Understanding** | Good (73% TextVQA) | "What biome is this?" |

### 3.2 Minecraft-Specific Use Cases

**OCR Capabilities (72.9% OCRBench):**
- Read text on signs
- Extract information from books
- Parse chat messages from screenshots
- Read scoreboard/objective text
- Decode crafting recipe UI text

**Visual Reasoning:**
- Identify block types from texture
- Recognize items in inventory
- Detect mobs and entities
- Navigate terrain features
- Identify structure types (village, temple, etc.)

**Multi-Image Analysis:**
- Compare before/after states
- Track changes over time
- Progressive construction verification
- Multi-angle scene analysis

### 3.3 Video Understanding

SmolVLM2 supports video understanding through frame sampling:
- **Default:** 50 uniformly sampled frames
- **Benchmark:** Video-MME (52.1% - beats InternVL2-2B)
- **Minecraft Use:** Record gameplay clips, analyze build progress

---

## 4. API Format for vLLM (localhost:8000)

### 4.1 Starting vLLM Server

```bash
# Basic vLLM server with SmolVLM
vllm serve HuggingFaceTB/SmolVLM-Instruct \
    --host localhost \
    --port 8000 \
    --chat-template template_llava.jinja

# With multi-image support
vllm serve HuggingFaceTB/SmolVLM-Instruct \
    --host localhost \
    --port 8000 \
    --max-model-len 4096 \
    --limit-mm-per-prompt image=4

# For GPU-constrained systems
vllm serve HuggingFaceTB/SmolVLM2-256M \
    --host localhost \
    --port 8000 \
    --dtype half \
    --gpu-memory-utilization 0.8
```

### 4.2 OpenAI-Compatible API Endpoint

**Base URL:** `http://localhost:8000/v1`
**Chat Completions:** `POST /chat/completions`

The vLLM server provides **100% OpenAI API compatibility** for multimodal requests.

---

## 5. Image Encoding Methods

### 5.1 Base64 Encoding (Recommended for Local Files)

```python
import base64
from pathlib import Path

def encode_image_to_base64(image_path: str) -> str:
    """Encode an image file to base64 string with MIME type prefix."""
    with open(image_path, "rb") as image_file:
        base64_data = base64.b64encode(image_file.read()).decode('utf-8')

    # Detect MIME type from extension
    ext = Path(image_path).suffix.lower()
    mime_type = {
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.png': 'image/png',
        '.webp': 'image/webp',
    }.get(ext, 'image/png')

    return f"data:{mime_type};base64,{base64_data}"

# Usage
base64_image = encode_image_to_base64("screenshot.png")
# Returns: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
```

### 5.2 Direct URL Format

```python
# For publicly accessible URLs
image_url = "https://example.com/screenshot.png"
```

### 5.3 Multi-Image Encoding

```python
def encode_multiple_images(image_paths: list[str]) -> list[dict]:
    """Encode multiple images for multimodal request."""
    return [
        {
            "type": "image_url",
            "image_url": {
                "url": encode_image_to_base64(path)
            }
        }
        for path in image_paths
    ]
```

---

## 6. Complete Code Examples

### 6.1 Python - OpenAI Client with Base64 Image

```python
import base64
from openai import OpenAI
from pathlib import Path

# Initialize OpenAI client pointing to local vLLM server
client = OpenAI(
    api_key="EMPTY",  # vLLM doesn't require a key
    base_url="http://localhost:8000/v1"
)

def encode_image(image_path: str) -> str:
    """Encode image to base64 with MIME prefix."""
    with open(image_path, "rb") as f:
        return f"data:image/png;base64,{base64.b64encode(f.read()).decode()}"

# Analyze a Minecraft screenshot
screenshot_path = "minecraft_screenshot.png"

response = client.chat.completions.create(
    model="HuggingFaceTB/SmolVLM-Instruct",
    messages=[{
        "role": "user",
        "content": [
            {
                "type": "text",
                "text": "Analyze this Minecraft screenshot. What biome is this? What resources are visible? Are there any dangers?"
            },
            {
                "type": "image_url",
                "image_url": {"url": encode_image(screenshot_path)}
            }
        ]
    }],
    max_tokens=512,
    temperature=0.7
)

analysis = response.choices[0].message.content
print(analysis)
```

### 6.2 Python - Multi-Image Comparison

```python
# Compare two screenshots for changes
response = client.chat.completions.create(
    model="HuggingFaceTB/SmolVLM-Instruct",
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "Compare these two Minecraft screenshots. What has changed between them?"},
            {"type": "image_url", "image_url": {"url": encode_image("before.png")}},
            {"type": "image_url", "image_url": {"url": encode_image("after.png")}}
        ]
    }],
    max_tokens=500
)
```

### 6.3 Python - Raw HTTP Requests

```python
import requests
import base64
import json

API_URL = "http://localhost:8000/v1/chat/completions"
MODEL_NAME = "HuggingFaceTB/SmolVLM-Instruct"

def encode_image(image_path: str) -> str:
    with open(image_path, "rb") as f:
        encoded = base64.b64encode(f.read()).decode('utf-8')
    return f"data:image/png;base64,{encoded}"

# OCR Example - Read text from a sign
payload = {
    "model": MODEL_NAME,
    "messages": [{
        "role": "user",
        "content": [
            {"type": "text", "text": "Read all the text visible in this image. Transcribe exactly what is written."},
            {"type": "image_url", "image_url": {"url": encode_image("sign_screenshot.png")}}
        ]
    }],
    "max_tokens": 256,
    "temperature": 0.1  # Lower temperature for precise OCR
}

response = requests.post(API_URL, json=payload)
result = response.json()
text_content = result['choices'][0]['message']['content']
print(f"OCR Result: {text_content}")
```

### 6.4 Java - Integration with Steve AI

```java
package com.steve.ai.vision;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * SmolVLM Vision Client for Minecraft AI Integration
 * Connects to local vLLM server at localhost:8000
 */
public class SmolVLMClient {

    private static final String API_BASE = "http://localhost:8000/v1";
    private static final String MODEL = "HuggingFaceTB/SmolVLM-Instruct";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SmolVLMClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Encode image file to base64 with MIME type prefix
     */
    public String encodeImage(Path imagePath) throws Exception {
        byte[] imageBytes = Files.readAllBytes(imagePath);
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = getMimeType(imagePath);
        return "data:" + mimeType + ";base64," + base64;
    }

    private String getMimeType(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        return "image/png"; // Default
    }

    /**
     * Analyze a Minecraft screenshot with SmolVLM
     */
    public String analyzeScreenshot(Path screenshotPath, String prompt) throws Exception {
        String base64Image = encodeImage(screenshotPath);

        // Build request body
        Map<String, Object> requestBody = Map.of(
            "model", MODEL,
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", List.of(
                        Map.of("type", "text", "text", prompt),
                        Map.of("type", "image_url",
                               "image_url", Map.of("url", base64Image))
                    )
                )
            ),
            "max_tokens", 512,
            "temperature", 0.7
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE + "/chat/completions"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("SmolVLM API error: " + response.body());
        }

        // Parse response
        Map<String, Object> responseBody = objectMapper.readValue(
            response.body(),
            Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    /**
     * OCR: Extract text from Minecraft sign/book
     */
    public String extractText(Path imagePath) throws Exception {
        return analyzeScreenshot(imagePath,
            "Read all text visible in this Minecraft screenshot. " +
            "Transcribe exactly what is written, preserving formatting."
        );
    }

    /**
     * Analyze biome and environment
     */
    public String analyzeEnvironment(Path imagePath) throws Exception {
        return analyzeScreenshot(imagePath,
            "Analyze this Minecraft screenshot. Identify the biome, " +
            "visible terrain features, resources, and any potential threats."
        );
    }
}
```

### 6.5 Java - Using with Existing Action System

```java
package com.steve.action.actions;

import com.steve.action.BaseAction;
import com.steve.ai.vision.SmolVLMClient;
import com.steve.entity.SteveEntity;
import com.steve.memory.TaskContext;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Vision-based action that uses SmolVLM to understand the environment
 */
public class VisionAnalyzeAction extends BaseAction {

    private final SmolVLMClient visionClient;
    private String analysisResult;
    private boolean analysisComplete = false;

    public VisionAnalyzeAction(SteveEntity steve, TaskContext task) {
        super(steve, task);
        this.visionClient = new SmolVLMClient();
    }

    @Override
    public void tick() {
        if (analysisComplete) return;

        try {
            // Take a screenshot using Minecraft's built-in screenshot function
            String timestamp = String.valueOf(System.currentTimeMillis());
            Path screenshotPath = Paths.get(
                "screenshots",
                "vision_analysis_" + timestamp + ".png"
            );

            // Trigger screenshot (implementation depends on Minecraft Forge API)
            takeScreenshot(screenshotPath);

            // Analyze with SmolVLM
            String prompt = task.getParameter("prompt",
                "What do you see in this Minecraft scene? " +
                "List blocks, entities, and notable features.");

            analysisResult = visionClient.analyzeScreenshot(screenshotPath, prompt);
            analysisComplete = true;

            // Store result in memory for future planning
            steve.getMemory().store("last_vision_analysis", analysisResult);

        } catch (Exception e) {
            setErrorState("Vision analysis failed: " + e.getMessage());
        }
    }

    private void takeScreenshot(Path path) {
        // Implementation: Use Minecraft's screenshot functionality
        // This would hook into the game's rendering pipeline
    }

    @Override
    public boolean isComplete() {
        return analysisComplete || hasError();
    }

    @Override
    public void onCancel() {
        // Cleanup if needed
    }
}
```

---

## 7. Performance Benchmarks

### 7.1 Inference Speed (NVIDIA A100 GPU)

| Batch Size | SmolVLM-256M | SmolVLM-2.2B |
|------------|--------------|--------------|
| 1 | 0.8 samples/sec | ~0.5 samples/sec |
| 8 | 6.2 samples/sec | ~3.0 samples/sec |
| 64 | 16.3 samples/sec | ~8.0 samples/sec |

### 7.2 Consumer GPU Performance (RTX 4060)

| Operation | Time |
|-----------|------|
| Single image analysis | 0.5 - 1.0 seconds |
| Multi-image comparison | 1.0 - 2.0 seconds |
| Video clip analysis (50 frames) | 5 - 10 seconds |

### 7.3 Task-Specific Benchmarks (SmolVLM2-2.2B)

| Benchmark | Score | Notes |
|-----------|-------|-------|
| **OCRBench** | 72.9% | Excellent for reading in-game text |
| **TextVQA** | 73.0% | Text in natural scenes |
| **DocVQA** | 80.0% | Document understanding |
| **ScienceQA** | 89.6% | Science questions |
| **Video-MME** | 52.1% | Video understanding (beats InternVL2-2B) |
| **WorldSense** | 36.2% | Beats Qwen2VL-7B (32.4%) |

### 7.4 Minecraft Task Estimates

Based on benchmarks, expected performance for Minecraft AI:

| Task | Estimated Time | Notes |
|------|----------------|-------|
| Read sign text | 0.5-1s | OCR capability |
| Identify biome | 0.5-1s | Visual classification |
| Count items in inventory | 1-2s | Object detection |
| Compare build progress | 1-2s | Multi-image analysis |
| Navigate to visible target | 0.5-1s | Spatial reasoning |

---

## 8. Recommended Use Cases for Minecraft AI

### 8.1 High-Impact Use Cases

#### 1. **Visual Navigation and Pathfinding**
```
Prompt: "Analyze this screenshot. Identify obstacles and suggest a safe path forward."
```
- Detect walls, cliffs, lava, water
- Identify safe routes
- Spot resources en route

#### 2. **OCR for Information Extraction**
```
Prompt: "Read all text from signs and books in this image."
```
- Read server rules
- Extract coordinates from signs
- Parse written book content
- Decode scoreboard objectives

#### 3. **Construction Verification**
```
Prompt: "Compare these before/after images. Is the structure built correctly according to the blueprint?"
```
- Verify build accuracy
- Identify missing blocks
- Check architectural details

#### 4. **Resource Identification**
```
Prompt: "List all visible ore blocks, trees, and resources in this area."
```
- Detect exposed ores
- Identify tree types
- Spot chest/loot locations

#### 5. **Threat Detection**
```
Prompt: "Identify any hostile mobs, dangerous terrain, or environmental threats."
```
- Spot mobs (zombies, creepers, etc.)
- Detect lava pools
- Identify mob spawners

#### 6. **Collaborative Building Analysis**
```
Prompt: "Analyze this partially built structure. What should be built next?"
```
- Determine next steps
- Identify missing components
- Suggest improvements

### 8.2 Advanced Multi-Modal Use Cases

#### Progressive Build Monitoring
- Take screenshots every N ticks
- Compare against blueprint
- Generate progress reports

#### Multi-Agent Scene Understanding
- Each agent shares visual perspective
- Collaborative spatial mapping
- Coordinated exploration

#### Video Replay Analysis
- Record gameplay clips
- Post-session analysis
- Performance optimization

### 8.3 Integration Points with Steve AI

| Steve AI Component | SmolVLM Integration |
|--------------------|---------------------|
| **TaskPlanner** | Add vision context to planning prompts |
| **SteveMemory** | Store visual analyses as world knowledge |
| **ActionExecutor** | Trigger vision actions for complex decisions |
| **CollaborativeBuildManager** | Verify construction with visual comparison |
| **Structure** | Generate NBT from image blueprints |

---

## 9. Deployment Recommendations

### 9.1 Model Selection

**For Development/Testing:**
- Use **SmolVLM2-256M** (~2GB VRAM)
- Fast iteration, acceptable quality

**For Production:**
- Use **SmolVLM2-2.2B** (~4.9GB VRAM)
- Best accuracy, still very efficient

**For Edge Deployment:**
- Use **SmolVLM2-256M** with quantization
- Can run on laptops, integrated GPUs

### 9.2 Server Configuration

```bash
# Recommended vLLM configuration for Minecraft AI
vllm serve HuggingFaceTB/SmolVLM2-2.2B \
    --host localhost \
    --port 8000 \
    --dtype half \
    --gpu-memory-utilization 0.8 \
    --max-model-len 4096 \
    --limit-mm-per-prompt image=4 \
    --chat-template template_llava.jinja
```

### 9.3 Error Handling

```java
// Implement fallback when vision API unavailable
try {
    String analysis = visionClient.analyzeScreenshot(screenshot, prompt);
} catch (Exception e) {
    // Fallback: Use LLM-only reasoning
    String analysis = llmClient.chat("I cannot see the image. Based on context...");
}
```

### 9.4 Caching Strategy

- Cache vision analyses with screenshot hash
- TTL: 5 minutes (game world changes)
- Invalidate on chunk load/unload

---

## 10. Limitations and Considerations

### 10.1 Known Limitations

| Limitation | Impact | Mitigation |
|------------|--------|------------|
| **Context Window** | 2048 tokens limits complex scenes | Break into multiple analyses |
| **Image Resolution** | Optimal: 384x384 to 512x512 | Resize screenshots appropriately |
| **Temporal Understanding** | Limited video reasoning | Use multi-frame comparison |
| **Fine Detail** | May miss small text/items | Zoom/crop regions of interest |

### 10.2 Minecraft-Specific Challenges

1. **Texture Packs:** Custom textures may confuse model
   - Mitigation: Fine-tune on Minecraft-specific textures

2. **Dynamic Lighting:** Dark caves may have poor visibility
   - Mitigation: Apply brightness adjustments pre-processing

3. **Motion Blur:** Fast movement may reduce quality
   - Mitigation: Take screenshots during still moments

4. **UI Overlay:** Hotbar, crosshair may interfere
   - Mitigation: Crop or mask UI elements

---

## 11. Future Enhancements

### 11.1 Fine-Tuning for Minecraft

```python
# Prepare Minecraft-specific training data
training_data = [
    {
        "image": "minecraft_screenshot_001.png",
        "text": "A plains biome with oak trees and a village visible in the distance."
    },
    # ... more examples
]

# Fine-tune SmolVLM on Minecraft-specific data
# This would improve accuracy for Minecraft-specific visual tasks
```

### 11.2 Specialized Vision Actions

- `OcrReadAction`: Read signs, books, chat
- `BiomeIdentifyAction`: Detect biome type
- `ResourceScanAction`: Find visible resources
- `ThreatDetectAction`: Spot hostile mobs
- `BuildVerifyAction`: Compare against blueprint

---

## 12. References and Resources

### Official Documentation
- [HuggingFace SmolVLM Model Card](https://huggingface.co/HuggingFaceTB/SmolVLM-Instruct)
- [SmolVLM GitHub Repository](https://github.com/huggingface/smollm)
- [vLLM Documentation](https://vllm.hyper.ai/docs/)
- [vLLM OpenAI Compatible Server](https://vllm.hyper.ai/docs/inference-and-serving/openai_compatible_server/)

### Research Papers
- Hugging Face SmolVLM Technical Report (2025)
- SigLIP: Sigmoid Loss for Language-Image Pre-training
- SmolLM2: Efficient Small Language Models

### Community Resources
- [SmolVLM Demo Space](https://huggingface.co/spaces/HuggingFaceTB/SmolVLM)
- [SmolVLM-256M Demo](https://huggingface.co/spaces/HuggingFaceTB/SmolVLM-256M-Demo)

---

## 13. Quick Start Checklist

For integrating SmolVLM into Steve AI:

- [ ] Install vLLM: `pip install vllm`
- [ ] Download model: `vllm serve HuggingFaceTB/SmolVLM-Instruct`
- [ ] Test basic inference with curl/Python
- [ ] Implement `SmolVLMClient.java` in Steve AI
- [ ] Create `VisionAnalyzeAction` extending `BaseAction`
- [ ] Add screenshot capture functionality
- [ ] Implement caching for vision results
- [ ] Test with Minecraft screenshots
- [ ] Benchmark on target hardware
- [ ] Deploy to production with monitoring

---

## Appendix: Complete API Reference

### vLLM Chat Completions API

**Endpoint:** `POST http://localhost:8000/v1/chat/completions`

**Request Schema:**
```json
{
  "model": "HuggingFaceTB/SmolVLM-Instruct",
  "messages": [
    {
      "role": "user",
      "content": [
        {"type": "text", "text": "Your prompt here"},
        {"type": "image_url", "image_url": {"url": "data:image/png;base64,..."}}
      ]
    }
  ],
  "max_tokens": 512,
  "temperature": 0.7,
  "stream": false
}
```

**Response Schema:**
```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "HuggingFaceTB/SmolVLM-Instruct",
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
    "prompt_tokens": 50,
    "completion_tokens": 100,
    "total_tokens": 150
  }
}
```

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Author:** Research for Steve AI Integration
