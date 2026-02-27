# NPU Integration Research for MineWright Mod

**Research Date:** 2026-02-26
**Hardware:** ProArt 13 laptop with Ryzen AI 9 HX (NPU), RTX 4050, 32GB RAM
**Objective:** Leverage AMD Ryzen AI NPU for local AI processing in MineWright Minecraft mod

---

## Executive Summary

**Feasibility:** LIMITED - No direct Java support for AMD NPU, but viable alternatives exist

**Recommendation:** Use a hybrid approach combining RTX 4050 (CUDA) for heavy inference and local embedding models for memory search, with future NPU integration possible via native bridges.

**Key Finding:** AMD's XDNA NPU has no official Java SDK. The Ryzen AI Software stack supports Python and C++ only. However, practical alternatives exist using the RTX 4050 GPU and CPU-based local models.

---

## Hardware Capabilities

### Ryzen AI 9 HX NPU Specifications

| Component | Specification |
|-----------|---------------|
| **Architecture** | XDNA (Gen 1+) or XDNA 2 |
| **Performance** | ~16-50+ TOPS (depending on exact SKU) |
| **Unified Memory** | Up to 128GB shared with CPU/GPU |
| **Supported Workloads** | Vision transformers, compact LLMs, embeddings |
| **Software Stack** | ONNX Runtime with Vitis AI Execution Provider |

### RTX 4050 GPU (Better Alternative for Java)

| Component | Specification |
|-----------|---------------|
| **CUDA Cores** | 2,560 |
| **VRAM** | 6GB GDDR6 |
| **Tensor Cores** | Yes (3rd Gen) |
| **Java Support** | Excellent via ONNX Runtime CUDA, DJL, llama.cpp |
| **Inference Speed** | 10-50x faster than NPU for most models |

---

## NPU Integration Analysis

### 1. What AI Workloads CAN Run on NPU?

Based on AMD Ryzen AI capabilities:

- **Embedding Models** (e.g., Nomic-embed-text-v1.5 - [AMD NPU optimized version](https://huggingface.co/amd/NPU-Nomic-embed-text-v1.5-ryzen-strix-cpp))
- **Lightweight LLMs** (1-3B parameter models)
- **Classification Tasks** (vision transformers, ResNet)
- **Stable Diffusion** (SD 3.0 Medium optimized for XDNA 2)
- **Whisper** (speech recognition)

**Workloads SUITABLE for MineWright:**
- Semantic search embeddings for memory retrieval
- Task intent classification
- Prompt preprocessing
- Response post-processing

**Workloads BETTER on RTX 4050:**
- Full LLM inference (task planning, action generation)
- Complex reasoning chains
- Multi-turn conversations

---

## Java Integration Possibilities

### Direct NPU Access: NOT FEASIBLE

**Problem:** AMD Ryzen AI SDK has NO Java support.

**Supported Languages:**
- Python (primary)
- C++ (via ONNX Runtime + XRT)

**Missing:**
- No Java bindings for XRT/XDNA
- No JNI wrappers provided by AMD
- No official Java SDK announced

---

## Practical Integration Approaches

### Approach 1: RTX 4050 (CUDA) - RECOMMENDED

**Pros:**
- Excellent Java support via ONNX Runtime CUDA
- 10-50x faster than NPU for most workloads
- Mature ecosystem (DJL, llama.cpp Java bindings)
- Works with existing AsyncLLMClient interface

**Cons:**
- Higher power consumption than NPU
- 6GB VRAM limits model size

**Implementation:**

```gradle
// Add to build.gradle
dependencies {
    // ONNX Runtime with CUDA support
    implementation 'com.microsoft.onnxruntime:onnxruntime_gpu:1.17.0'

    // Alternative: DJL with PyTorch engine
    implementation 'ai.djl.pytorch:pytorch-engine:0.23.0'
    implementation 'ai.djl.pytorch:pytorch-native-cu118:0.23.0'
}
```

**Code Example: Local LLM with llama.cpp Java**

```java
package com.minewright.llm.local;

import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Local LLM client using llama.cpp Java bindings.
 * Runs on RTX 4050 GPU via CUDA.
 */
public class LocalLLMClient implements AsyncLLMClient {

    private final LlamaModel model;
    private final String modelPath;

    public LocalLLMClient(String modelPath) {
        this.modelPath = modelPath;
        // Load GGUF model (e.g., mistral-7b-instruct-v0.2.Q4_K_M.gguf)
        this.model = new LlamaModel.Builder()
            .setModelPath(modelPath)
            .setNGPULayers(32)  // Offload to RTX 4050
            .setNThreads(8)
            .build();
    }

    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int maxTokens = (Integer) params.getOrDefault("maxTokens", 1000);
                double temperature = (Double) params.getOrDefault("temperature", 0.7);

                String response = model.generate(
                    prompt,
                    maxTokens,
                    temperature,
                    new LlamaSampler.ChainSampler()
                        .temperature(temperature)
                        .topK(40)
                        .topP(0.95)
                );

                return new LLMResponse(response, Map.of("source", "local-gpu"));

            } catch (Exception e) {
                throw new RuntimeException("Local inference failed", e);
            }
        }, LLMExecutorService.getExecutor("local"));
    }

    @Override
    public String getProviderId() {
        return "local-gpu";
    }

    @Override
    public boolean isHealthy() {
        return model != null && model.isLoaded();
    }
}
```

---

### Approach 2: ONNX Runtime Java (CPU/GPU)

**Pros:**
- Official Microsoft support
- Works with CPU, CUDA, and DirectML
- Can run embedding models efficiently

**Cons:**
- No NPU execution provider for Java
- Limited model format support (ONNX only)

**Implementation:**

```gradle
dependencies {
    implementation 'com.microsoft.onnxruntime:onnxruntime:1.17.0'
}
```

**Code Example: Local Embedding Model**

```java
package com.minewright.llm.embeddings;

import ai.onnxruntime.*;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * Local embedding model using ONNX Runtime.
 * Generates semantic embeddings for memory search.
 *
 * Model: all-MiniLM-L6-v2.onnx (384 dimensions)
 * Alternative: Nomic-embed-text-v1.5 (converted to ONNX)
 */
public class LocalEmbeddingModel {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final OrtSession.Result metadata;

    public LocalEmbeddingModel(String modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();

        // Use CUDA if available, otherwise CPU
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        if (isCudaAvailable()) {
            options.addCUDA(0);  // Use RTX 4050
        }

        this.session = env.createSession(modelPath, options);
        this.metadata = session.getInputNames();
    }

    public float[] generateEmbedding(String text) throws OrtException {
        // Tokenize text (simplified - use proper tokenizer in production)
        long[] inputIds = tokenize(text);

        // Create input tensor
        long[] shape = {1, inputIds.length};
        OnnxTensor inputTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(inputIds),
            shape
        );

        // Run inference
        OrtSession.Result result = session.run(
            Map.of("input_ids", inputTensor)
        );

        // Extract embedding (pooling layer output)
        float[][] embeddings = (float[][]) result.get(0).getValue();
        return embeddings[0];
    }

    public double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private boolean isCudaAvailable() {
        try {
            // Check for CUDA availability
            return OrtEnvironment.getEnvironment()
                .getProviders()
                .contains("CUDAExecutionProvider");
        } catch (Exception e) {
            return false;
        }
    }

    private long[] tokenize(String text) {
        // Simplified tokenization
        // In production, use proper tokenizer (e.g., ByteLevelBPETokenizer)
        return Arrays.stream(text.split("\\s+"))
            .mapToLong(String::hashCode)
            .toArray();
    }
}
```

---

### Approach 3: JNI Bridge to AMD NPU - EXPERIMENTAL

**Pros:**
- Direct NPU access possible
- Lowest power consumption

**Cons:**
- Complex setup (build XRT, write JNI wrappers)
- No official support
- High development effort
- Future compatibility uncertain

**Implementation Path:**

1. **Install AMD XDNA drivers**
   ```bash
   # Linux kernel 6.14+ includes amdxdna driver
   # Or install AMD Ryzen AI Software
   ```

2. **Build C++ wrapper** using ONNX Runtime + Vitis AI EP

3. **Create JNI bindings**

```cpp
// native/src/npu_wrapper.cpp
#include <onnxruntime_cxx_api.h>
#include <jni.h>

extern "C" JNIEXPORT jlong JNICALL
Java_com_minewright_ai_llm_npu_NPUClient_createSession(
    JNIEnv* env,
    jobject obj,
    jstring modelPath
) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    // Create ONNX Runtime session with Vitis AI EP
    Ort::Env env Ort(ORT_LOGGING_LEVEL_WARNING, "MineWrightAI");
    Ort::SessionOptions session_options;
    session_options.AppendExecutionProvider_VitisAI();

    Ort::Session* session = new Ort::Session(env, path, session_options);

    env->ReleaseStringUTFChars(modelPath, path);
    return reinterpret_cast<jlong>(session);
}
```

4. **Java side**

```java
package com.minewright.llm.npu;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * NPU client via JNI bridge to ONNX Runtime + Vitis AI EP.
 *
 * WARNING: This is experimental and requires:
 * 1. AMD Ryzen AI Software installed
 * 2. Native library compiled for your platform
 * 3. XRT/XDNA drivers loaded
 *
 * Status: NOT RECOMMENDED for production
 */
public class NPUClient {

    static {
        try {
            // Load compiled JNI library
            System.loadLibrary("minewright_npu_wrapper");
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException(
                "NPU native library not found. NPU support unavailable.", e
            );
        }
    }

    private final long sessionHandle;

    public NPUClient(String modelPath) {
        if (!isNPUSupported()) {
            throw new UnsupportedOperationException(
                "NPU is not available on this system"
            );
        }
        this.sessionHandle = createSession(modelPath);
    }

    // Native methods
    private native long createSession(String modelPath);
    private native float[] runInference(long handle, float[] input);
    private native void closeSession(long handle);

    public static boolean isNPUSupported() {
        // Check for AMD NPU presence
        try {
            String cpuInfo = Files.readString(Paths.get("/proc/cpuinfo"));
            return cpuInfo.contains("AMD") &&
                   Files.exists(Paths.get("/dev/amdxdna"));
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## Integration with MineWright Architecture

### Proposed Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    MineWright Mod                              │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐    ┌─────────────────────────────────┐   │
│  │  Press K    │───▶│      TaskPlanner                │   │
│  │  (GUI)      │    │  - Detects local vs cloud       │   │
│  └──────────────┘    │  - Routes to appropriate client │   │
│                      └─────────────────────────────────┘   │
│                                  │                          │
│                   ┌──────────────┼──────────────┐          │
│                   ▼              ▼              ▼          │
│           ┌───────────┐  ┌─────────────┐  ┌─────────┐     │
│           │   Cloud   │  │   Local GPU │  │   CPU   │     │
│           │   LLM     │  │   (RTX4050) │  │Fallback │     │
│           │(OpenAI/   │  │  llama.cpp  │  │         │     │
│           │ Groq/Gem) │  │  ONNX RT    │  │         │     │
│           └───────────┘  └─────────────┘  └─────────┘     │
│                   │              │              │          │
│                   └──────────────┼──────────────┘          │
│                                  ▼                          │
│                    ┌─────────────────────────┐             │
│                    │   EmbeddingService      │             │
│                    │   (Local: ONNX model)   │             │
│                    └─────────────────────────┘             │
│                                  │                          │
│                                  ▼                          │
│                    ┌─────────────────────────┐             │
│                    │   ForemanMemory          │             │
│                    │   (Semantic Search)    │             │
│                    └─────────────────────────┘             │
└─────────────────────────────────────────────────────────────┘
```

---

## Recommended Implementation Plan

### Phase 1: Local Embedding Model (Immediate)

**Objective:** Accelerate memory search with local embeddings

**Tech Stack:** ONNX Runtime Java

**Model:** `all-MiniLM-L6-v2` (384 dim, 80MB) or `Nomic-embed-text-v1.5` (768 dim)

**Implementation:**

```java
package com.minewright.memory;

import com.minewright.llm.embeddings.LocalEmbeddingModel;

/**
 * Enhanced ForemanMemory with semantic search.
 */
public class ForemanMemory {

    private final LocalEmbeddingModel embeddingModel;
    private final List<MemoryEntry> memories;

    public ForemanMemory() {
        try {
            this.embeddingModel = new LocalEmbeddingModel(
                "models/all-MiniLM-L6-v2.onnx"
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load embedding model", e);
        }
    }

    public List<MemoryEntry> searchRelevant(String query, int topK) {
        try {
            float[] queryEmbedding = embeddingModel.generateEmbedding(query);

            return memories.stream()
                .map(entry -> Map.entry(
                    entry,
                    embeddingModel.cosineSimilarity(
                        queryEmbedding,
                        entry.getEmbedding()
                    )
                ))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        } catch (Exception e) {
            MineWrightMod.LOGGER.error("Semantic search failed", e);
            return Collections.emptyList();
        }
    }

    public void addMemory(String content, String type) {
        try {
            float[] embedding = embeddingModel.generateEmbedding(content);
            memories.add(new MemoryEntry(content, type, embedding));
        } catch (Exception e) {
            MineWrightMod.LOGGER.error("Failed to embed memory", e);
        }
    }
}
```

**Benefits:**
- Fast, offline memory search
- Better context retrieval for task planning
- No API costs
- Low latency (< 50ms per search)

---

### Phase 2: Local LLM with RTX 4050 (Short-term)

**Objective:** Run task planning locally without cloud APIs

**Tech Stack:** llama.cpp Java bindings

**Model:** `mistral-7b-instruct-v0.2.Q4_K_M.gguf` (4.3GB, fits in 6GB VRAM)

**Implementation:**

```java
package com.minewright.llm.local;

import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Local LLM provider using llama.cpp Java bindings.
 * Offloads computation to RTX 4050 GPU.
 */
public class LocalLLMProvider implements AsyncLLMClient {

    private final LlamaModel model;

    public LocalLLMProvider() {
        // Download model:
        // https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF
        this.model = new LlamaModel.Builder()
            .setModelPath("models/mistral-7b-instruct-v0.2.Q4_K_M.gguf")
            .setNGPULayers(32)  // Offload to RTX 4050 (6GB VRAM)
            .setNThreads(8)
            .setContextLength(4096)
            .build();
    }

    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            String response = model.generate(
                prompt,
                (Integer) params.getOrDefault("maxTokens", 1000),
                (Double) params.getOrDefault("temperature", 0.7)
            );
            return new LLMResponse(response, Map.of("source", "local-gpu"));
        }, LLMExecutorService.getExecutor("local"));
    }

    @Override
    public String getProviderId() {
        return "local-gpu";
    }

    @Override
    public boolean isHealthy() {
        return model != null;
    }
}
```

**Expected Performance:**
- **Token Generation:** 30-50 tokens/second on RTX 4050
- **Latency:** ~200ms first token, ~20ms subsequent tokens
- **VRAM Usage:** ~5GB for 7B Q4 model
- **Power:** ~30-50W under load

---

### Phase 3: Hybrid Cloud/Local Fallback (Medium-term)

**Objective:** Seamlessly switch between local and cloud LLMs

**Implementation:**

```java
package com.minewright.llm.hybrid;

import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Hybrid LLM provider that tries local first, falls back to cloud.
 */
public class HybridLLMProvider implements AsyncLLMClient {

    private final AsyncLLMClient localProvider;
    private final AsyncLLMClient cloudProvider;
    private final CircuitBreaker circuitBreaker;

    public HybridLLMProvider() {
        this.localProvider = new LocalLLMProvider();
        this.cloudProvider = new AsyncOpenAIClient(apiKey, model, maxTokens, temperature);

        this.circuitBreaker = CircuitBreaker.ofDefaults("local-llm");
        this.circuitBreaker.getEventPublisher()
            .onError(e -> MineWrightMod.LOGGER.warn("Local LLM failed, using cloud"));
    }

    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        // Try local first
        if (circuitBreaker.tryAcquirePermission()) {
            return localProvider.sendAsync(prompt, params)
                .exceptionally(throwable -> {
                    circuitBreaker.onError(0, throwable);
                    // Fallback to cloud
                    return cloudProvider.sendAsync(prompt, params).join();
                });
        } else {
            // Circuit open, use cloud directly
            return cloudProvider.sendAsync(prompt, params);
        }
    }

    @Override
    public String getProviderId() {
        return "hybrid";
    }

    @Override
    public boolean isHealthy() {
        return localProvider.isHealthy() || cloudProvider.isHealthy();
    }
}
```

---

## NPU Integration: Future Considerations

### When to Consider NPU Integration

**AMD releases official Java SDK** - Monitor:
- [AMD Ryzen AI Developer Portal](https://www.amd.com/en/developer/resources/ryzen-ai-software.html)
- [RyzenAI-SW GitHub](https://github.com/amd/RyzenAI-SW)

**Signs to watch for:**
- Java bindings for ONNX Runtime Vitis AI EP
- JNI libraries for XRT/XDNA
- Official documentation for Java NPU usage

### Current Workaround for NPU

If you want to experiment with NPU today:

1. **Python Microservice Approach**

```python
# npu_service.py
from ryzen_ai import RyzenAI
from fastapi import FastAPI
import uvicorn

app = FastAPI()

# Initialize NPU
npu = RyzenAI()
npu.load_model("models/nomic-embed-text-v1.5-ryzen.onnx")

@app.post("/embed")
async def embed(text: str):
    embedding = npu.run(text)
    return {"embedding": embedding.tolist()}

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8080)
```

2. **Java Client**

```java
package com.minewright.llm.npu;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * NPU service client via Python microservice.
 */
public class NPUServiceClient {

    private final HttpClient client = HttpClient.newHttpClient();

    public float[] generateEmbedding(String text) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:8080/embed"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"text\": \"" + text + "\"}"
                ))
                .build();

            HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            // Parse JSON response
            return parseEmbedding(response.body());

        } catch (Exception e) {
            throw new RuntimeException("NPU service call failed", e);
        }
    }
}
```

**Benefits:**
- Leverages AMD's official Python SDK
- NPU acceleration for embeddings
- Low power consumption

**Drawbacks:**
- Requires Python environment
- Adds network overhead
- More complex deployment

---

## Performance Comparison

### Expected Latency (Single Request)

| Task | Cloud API | Local GPU (RTX 4050) | Local CPU | NPU (via Python) |
|------|-----------|----------------------|-----------|------------------|
| **7B LLM Inference** | 500-2000ms | 200-500ms | 5000ms+ | N/A (not supported) |
| **Embedding Generation** | 200-500ms | 50-100ms | 200-500ms | 50-100ms |
| **Memory Search (10k items)** | 100-300ms | 20-50ms | 100-300ms | 20-50ms |
| **Task Planning** | 1-3s | 300ms-1s | 5-10s | N/A |

### Power Consumption

| Configuration | Idle | Load |
|---------------|------|------|
| **Cloud API only** | 5W | 10W (network) |
| **RTX 4050 inference** | 15W | 50-80W |
| **NPU inference** | 5W | 10-15W |
| **CPU inference** | 10W | 30-50W |

---

## Recommended Configuration

### For MineWright Mod Development

```toml
# config/minewright-common.toml

[llm]
# Primary: Local GPU (fastest, lowest latency)
provider = "local-gpu"
# Fallback: Cloud API (for complex tasks)
fallback = "groq"

[local]
# Model path (relative to mod directory)
modelPath = "models/mistral-7b-instruct-v0.2.Q4_K_M.gguf"
# GPU layers to offload (adjust based on VRAM)
gpuLayers = 32
# Threads for CPU fallback
threads = 8

[embeddings]
# Local embedding model for semantic search
modelPath = "models/all-MiniLM-L6-v2.onnx"
dimensions = 384
# Use GPU if available
useGPU = true
```

---

## Build Configuration

### Add to build.gradle

```gradle
dependencies {
    // Existing dependencies...

    // Local LLM support (optional, user can exclude)
    implementation 'com.github.devoxygen:java-llama.cpp:1.0.0' {
        // Allow users to exclude native libraries
        exclude group: 'org.lwjgl'
    }

    // ONNX Runtime for embeddings
    implementation 'com.microsoft.onnxruntime:onnxruntime:1.17.0'

    // Configuration for local models
    implementation 'com.typesafe:config:1.4.3'
}

// Task to download models (optional)
task downloadModels(type: DownloadModelsTask) {
    models = [
        'https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf',
        'https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/model.onnx'
    ]
    outputDir = file('models')
}
```

---

## Testing Strategy

### Unit Tests

```java
@Test
void testLocalEmbeddingGeneration() throws OrtException {
    LocalEmbeddingModel model = new LocalEmbeddingModel("models/test-model.onnx");

    float[] embedding1 = model.generateEmbedding("test");
    float[] embedding2 = model.generateEmbedding("test");

    // Same input should produce same embedding
    assertArrayEquals(embedding1, embedding2, 0.001f);
}

@Test
void testSemanticSearch() {
    ForemanMemory memory = new ForemanMemory();
    memory.addMemory("minewright mined diamond", "action");
    memory.addMemory("minewright built house", "action");

    List<MemoryEntry> results = memory.searchRelevant("mining resources", 1);

    assertEquals("minewright mined diamond", results.get(0).getContent());
}
```

### Integration Tests

```java
@Test
void testHybridProviderFallback() {
    HybridLLMProvider provider = new HybridLLMProvider();

    // Local provider should work
    CompletableFuture<LLMResponse> response1 = provider.sendAsync(
        "Say hello", Map.of("maxTokens", 10)
    );
    assertNotNull(response1.join());

    // Break local provider
    ((LocalLLMProvider) provider.localProvider).close();

    // Should fallback to cloud
    CompletableFuture<LLMResponse> response2 = provider.sendAsync(
        "Say hello", Map.of("maxTokens", 10)
    );
    assertNotNull(response2.join());
}
```

---

## Migration Path

### Current State (All Cloud)
```
User → Cloud API (OpenAI/Groq/Gemini) → TaskPlanner → ActionExecutor
```

### Target State (Hybrid)
```
User → TaskPlanner → [Local GPU (primary) | Cloud (fallback)] → ActionExecutor
              ↓
       EmbeddingService (local ONNX)
              ↓
       ForemanMemory (semantic search)
```

### Rollout Strategy

1. **Week 1-2:** Add local embedding service
   - Deploy ONNX embedding model
   - Integrate with ForemanMemory
   - A/B test vs. no embeddings

2. **Week 3-4:** Add local LLM option
   - Deploy llama.cpp Java bindings
   - Add configuration option
   - Test with small models first

3. **Week 5-6:** Hybrid provider
   - Implement fallback logic
   - Add circuit breaker
   - Gradually shift traffic to local

4. **Week 7+:** Monitor and optimize
   - Track latency, accuracy, cost
   - Tune model selection
   - Consider NPU when available

---

## Conclusion

### Summary of Findings

1. **AMD NPU Integration:** Not feasible directly due to lack of Java SDK
2. **RTX 4050 GPU:** Excellent alternative with mature Java support
3. **Local Embeddings:** High ROI, easy to implement with ONNX Runtime
4. **Local LLM:** Viable with llama.cpp, provides 5-10x latency improvement
5. **Hybrid Approach:** Best of both worlds - local speed + cloud capability

### Recommended Next Steps

1. **Start with embeddings:**
   - Download all-MiniLM-L6-v2.onnx (80MB)
   - Implement LocalEmbeddingModel
   - Add semantic search to ForemanMemory

2. **Evaluate local LLM:**
   - Download mistral-7b-instruct-v0.2.Q4_K_M.gguf (4.3GB)
   - Test with llama.cpp Java bindings
   - Measure quality vs. cloud

3. **Monitor AMD for Java SDK:**
   - Subscribe to Ryzen AI developer updates
   - Test when/if Java bindings released

### Expected Benefits

- **5-10x faster** task planning (local vs cloud)
- **90% cost reduction** (fewer API calls)
- **Offline capability** (no internet required)
- **Privacy:** Data never leaves device
- **Future-proof:** Ready for NPU when Java support arrives

---

## Sources

- [AMD Ryzen AI Software](https://www.amd.com/en/developer/resources/ryzen-ai-software.html)
- [AMD NPU-Nomic-embed-text-v1.5-ryzen-strix-cpp](https://huggingface.co/amd/NPU-Nomic-embed-text-v1.5-ryzen-strix-cpp)
- [AMD XDNA Debian Wiki](https://wiki.debian.org/XDNA)
- [AMD XDNA Gentoo Wiki](https://wiki.gentoo.org/wiki/User:Lockal/AMDXDNA)
- [Deep Java Library (DJL)](https://djl.ai)
- [ONNX Runtime Java](https://onnxruntime.ai/docs/api/java/)
- [java-llama.cpp](https://github.com/devoxygen/java-llama.cpp)
- [llama.cpp](https://github.com/ggerganov/llama.cpp)
- [RyzenAI-SW GitHub](https://github.com/amd/RyzenAI-SW)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Status:** Ready for Implementation
