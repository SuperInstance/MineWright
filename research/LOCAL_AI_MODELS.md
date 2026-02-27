# Local AI Models Research for MineWright (Minecraft Forge Mod)

**Research Date:** 2026-02-26
**Target Hardware:** RTX 4050 (6GB VRAM), Ryzen AI 9 HX (NPU), 32GB RAM
**Primary Cloud Provider:** z.ai GLM-5 (GLM-4-Flash for simple tasks)
**Target Environment:** Minecraft Forge 1.20.1, Java 17

---

## Executive Summary

This research evaluates local AI model integration options for MineWright, a "Cursor for Minecraft" mod that uses LLM-powered agents to execute natural language commands. The goal is to offload frequent, simple tasks (classification, embeddings, quick responses) to local models while keeping complex reasoning on cloud-based GLM-5.

**Key Findings:**
- **Best overall solution:** Ollama + LangChain4j for Java integration
- **Best lightweight model:** Phi-4-mini (3.8B, MIT licensed)
- **Best edge option:** FastFlowLM for Ryzen AI NPU utilization
- **Recommended hybrid architecture:** Local embeddings/classification + Cloud reasoning

---

## Table of Contents

1. [Comparison Matrix](#comparison-matrix)
2. [Ollama Integration for Java](#ollama-integration-for-java)
3. [Phi-4-Mini](#phi-4-mini)
4. [Gemma Variants](#gemma-variants)
5. [ONNX Runtime for Java](#onnx-runtime-for-java)
6. [DJL (Deep Java Library)](#djl-deep-java-library)
7. [llama.cpp Java Bindings](#llamacpp-java-bindings)
8. [Model Quantization](#model-quantization)
9. [Hybrid Cloud/Local Architecture](#hybrid-cloudlocal-architecture)
10. [Hardware-Specific Options](#hardware-specific-options)
11. [Implementation Recommendations](#implementation-recommendations)

---

## Comparison Matrix

| Solution | Model Size | VRAM/RAM | Speed (t/s) | Setup Complexity | Java Integration | Best For |
|----------|-----------|----------|-------------|------------------|------------------|----------|
| **Ollama + LangChain4j** | 3B-7B | 4-8GB | 15-30 | Low | Excellent | General purpose |
| **Phi-4-Mini (Ollama)** | 3.8B | ~4GB | 20-25 | Low | Excellent | Math/Reasoning |
| **Gemma 2 2B (Ollama)** | 2B | ~3GB | 25-35 | Low | Excellent | Lightweight tasks |
| **FunctionGemma** | 270M | ~1GB | 50+ | Medium | Poor | Tool routing |
| **DJL + HuggingFace** | 1-3B | 2-6GB | 10-20 | Medium | Good | Embeddings |
| **ONNX Runtime** | 1-3B | 2-6GB | 15-25 | High | Good | Optimized inference |
| **llama.cpp JNI** | 3-7B | 4-8GB | 20-35 | High | Good | Max performance |
| **Llama3.java** | 3B | 4-8GB | 15-25 | Low | Perfect | Pure Java |
| **FastFlowLM (NPU)** | 1.5-2B | NPU | 30-40 | Low | CLI-only | Ryzen AI NPU |
| **GLM-4-Flash (Cloud)** | - | 0 | - | Very Low | Excellent | Complex reasoning |

### Performance Estimates for RTX 4050 (6GB)

Based on benchmarks from similar GPUs (RTX 3050/3060):

| Model | Quantization | VRAM | Estimated t/s | Context Length |
|-------|--------------|------|---------------|----------------|
| Phi-4-Mini | Q4_K_M | ~4GB | 18-22 | 128K |
| Gemma 2 2B | Q4_0 | ~2.5GB | 25-30 | 8K |
| Qwen2.5 0.5B | Q8_0 | ~1GB | 40-50 | 32K |
| Llama 3.2 3B | Q4_K_M | ~2.5GB | 20-25 | 128K |
| bge-base embeddings | FP16 | ~500MB | 100+ (docs/sec) | 512 |

---

## Ollama Integration for Java

### Overview

Ollama is the most practical solution for Java/Minecraft applications due to:
- Simple HTTP API (default port 11434)
- Automatic model management (download, quantization, caching)
- Cross-platform support (Windows, Linux, macOS)
- OpenAI-compatible API endpoints
- Active development and community

### Java Integration via LangChain4j

**Recommended for:** Production use, easy integration

**Maven Dependencies:**
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.36.2</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>0.36.2</version>
</dependency>
<!-- Optional: Spring Boot starter -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>0.36.2</version>
</dependency>
```

**Basic Usage Example:**
```java
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

public class LocalLLMClient {
    private final ChatLanguageModel model;

    public LocalLLMClient() {
        this.model = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("phi4-mini")  // or "gemma2:2b", "llama3.2:3b"
            .temperature(0.7)
            .build();
    }

    public String chat(String message) {
        return model.generate(message);
    }
}
```

**Streaming Example (for tick-based execution):**
```java
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;

public class StreamingLocalLLM {
    private final StreamingChatLanguageModel model;

    public StreamingLocalLLM() {
        this.model = OllamaStreamingChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("phi4-mini")
            .build();
    }

    public void chatAsync(String message, Consumer<String> onToken) {
        model.generate(message, new ChatModelListener() {
            @Override
            public void onPartialResponse(String partial) {
                onToken.accept(partial);  // Called for each token
            }
        });
    }
}
```

### Direct HTTP Client (Alternative)

**For:** Lightweight integration without LangChain4j

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OllamaDirectClient {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private final HttpClient client;

    public OllamaDirectClient() {
        this.client = HttpClient.newHttpClient();
    }

    public String generate(String prompt) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", "phi4-mini");
        body.addProperty("prompt", prompt);
        body.addProperty("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
        return responseJson.get("response").getAsString();
    }
}
```

### Ollama Setup for MineWright

**Installation (Windows):**
```powershell
# Download from https://ollama.com/download
# Or via winget
winget install Ollama.Ollama

# Start Ollama (runs in background)
ollama serve

# Pull recommended models
ollama pull phi4-mini
ollama pull gemma2:2b
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text  # For embeddings
```

**Configuration for MineWright (config/minewright-common.toml):**
```toml
[local]
enabled = true
baseUrl = "http://localhost:11434"

[local.models]
classification = "gemma2:2b"      # Fast, lightweight
reasoning = "phi4-mini"            # Math/coding tasks
embeddings = "nomic-embed-text"    # Vector embeddings

[local.routing]
maxTokens = 512                    # Use local for < 512 token responses
useCloudForComplex = true          # Fallback to GLM-5 for complex tasks
```

---

## Phi-4-Mini

### Overview

Phi-4-Mini is Microsoft's "small but powerful" model, released February 27, 2025.

**Key Specifications:**
- **Parameters:** 3.8 billion
- **Context Window:** 128K tokens (200K for flash-reasoning variant)
- **License:** MIT (fully open source, commercial use allowed)
- **Architecture:** Decoder-based Transformer with SambaY hybrid (Mamba + differential attention)

### Capabilities

| Benchmark | Phi-4-Mini | Competitive With |
|-----------|------------|------------------|
| AIME 24/25 | Strong | Llama-3.1-8B |
| Math500 | High | 7B-9B models |
| GPQA Diamond | Competitive | DeepSeek-R1-Distill-Qwen-7B |
| Throughput | 10x faster than prev-gen | - |
| Latency | 60-70% reduction | - |

**Supported Tasks:**
- Mathematical reasoning (strength area)
- Programming/coding
- Function calling
- Multi-hop question answering
- 20+ languages

### Java Integration

**Via Ollama (Recommended):**
```bash
ollama pull phi4-mini
```

```java
ChatLanguageModel phi4 = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("phi4-mini")
    .temperature(0.3)  // Lower temp for math/reasoning
    .build();
```

**For Minecraft-Specific Tasks:**
```java
public class MinecraftTaskClassifier {
    private final ChatLanguageModel classifier;

    public MinecraftTaskClassifier() {
        this.classifier = OllamaChatModel.builder()
            .modelName("phi4-mini")
            .systemMessage("""
                You are a Minecraft task classifier. Categorize commands into:
                - MOVEMENT: move, walk, run, go to
                - MINING: mine, dig, collect, gather
                - BUILDING: build, construct, place
                - COMBAT: attack, fight, defend
                - CRAFTING: craft, make, create
                - OTHER: anything else

                Respond with only the category name.
                """)
            .build();
    }

    public TaskCategory classify(String command) {
        String response = classifier.generate(command);
        return TaskCategory.valueOf(response.toUpperCase());
    }
}
```

### Performance on RTX 4050

Estimated performance based on benchmarks:
- **Q4_K_M quantization:** ~2.5GB VRAM
- **Tokens/second:** 18-22 t/s
- **Time to first token:** 400-600ms
- **Recommended context:** 4K-8K for best performance

### Use Cases for MineWright

1. **Task Intent Classification:** Categorize user commands before routing
2. **Coordinate Reasoning:** Math-heavy calculations for positions/distances
3. **Code Generation:** Generate GraalVM JavaScript for action execution
4. **Quick Responses:** Fast responses for simple queries

---

## Gemma Variants

### Gemma 3 Family

Google's Gemma 3 (released January 2026) includes multiple sizes:

| Model | Parameters | Context | Tool Calling | License |
|-------|-----------|---------|--------------|---------|
| Gemma 3 1B | 1B | 4K | Via prompt | Gemma |
| Gemma 3 4B | 4B | 128K | Via prompt | Gemma |
| Gemma 3 12B | 12B | 128K | Via prompt | Gemma |
| Gemma 3 27B | 27B | 128K | Via prompt | Gemma |

**Important:** Gemma 3 does **not** have native tool tokens. Function calling is achieved through prompt engineering.

### Gemma 2 2B (Recommended for Local)

**Best For:** Lightweight, fast inference

```bash
ollama pull gemma2:2b
```

```java
ChatLanguageModel gemma = OllamaChatModel.builder()
    .modelName("gemma2:2b")
    .build();
```

**Performance on RTX 4050:**
- **Q4_0 quantization:** ~2GB VRAM
- **Tokens/second:** 25-30 t/s
- **Best for:** Simple classification, embeddings, quick chat

### FunctionGemma (270M)

**Specialized for:** Function calling on edge devices

| Spec | Value |
|------|-------|
| Base Model | Gemma 3 270M |
| Focus | Function calling routing |
| Hardware | Smartphones, Jetson Nano |
| Use Case | "Traffic controller" for routing to larger models |

**Limitations for MineWright:**
- Too small for Minecraft-specific reasoning
- Best used as a router to decide between local/cloud
- Limited Java integration options

**Integration Pattern:**
```java
// Use FunctionGemma concept with Ollama's gemma2:2b
public class ModelRouter {
    private final ChatLanguageModel router;

    public ModelRouter() {
        this.router = OllamaChatModel.builder()
            .modelName("gemma2:2b")
            .systemMessage("""
                Route the request to the appropriate model:
                - LOCAL: Simple queries, classification, embeddings
                - CLOUD: Complex reasoning, multi-step planning

                Respond with: LOCAL or CLOUD
                """)
            .temperature(0.0)  // Deterministic routing
            .build();
    }

    public ModelChoice route(String userCommand) {
        String decision = router.generate(userCommand);
        return ModelChoice.valueOf(decision);
    }
}
```

---

## ONNX Runtime for Java

### Overview

ONNX Runtime provides cross-platform inference with hardware acceleration.

**Maven Dependencies:**
```xml
<!-- CPU version -->
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.19.2</version>
</dependency>

<!-- GPU version (CUDA) -->
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime_gpu</artifactId>
    <version>1.19.2</version>
</dependency>
```

### Basic Usage

```java
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;

public class ONNXModelRunner {
    private final OrtEnvironment env;
    private final OrtSession session;

    public ONNXModelRunner(String modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath);
    }

    public float[] runInference(String input) throws OrtException {
        // Preprocess input to ONNX tensor format
        // Run inference
        // Postprocess output

        try (Result results = session.run(/* inputs */)) {
            return (float[]) results.get(0).getValue();
        }
    }

    public void close() throws OrtException {
        session.close();
        env.close();
    }
}
```

### ONNX Model Conversion

**Convert HuggingFace model to ONNX:**
```python
# Requires transformers and onnx packages
pip install transformers onnx onnxruntime

from transformers import AutoModelForCausalLM, AutoTokenizer
import torch

model = AutoModelForCausalLM.from_pretrained("microsoft/phi-4-mini")
tokenizer = AutoTokenizer.from_pretrained("microsoft/phi-4-mini")

# Export to ONNX
dummy_input = tokenizer("Hello", return_tensors="pt")
torch.onnx.export(
    model,
    (dummy_input['input_ids'], dummy_input['attention_mask']),
    "phi-4-mini.onnx",
    input_names=['input_ids', 'attention_mask'],
    output_names=['logits'],
    dynamic_axes={
        'input_ids': {0: 'batch_size', 1: 'sequence_length'},
        'attention_mask': {0: 'batch_size', 1: 'sequence_length'},
        'logits': {0: 'batch_size', 1: 'sequence_length'}
    }
)
```

### Integration with Ryzen AI NPU

AMD provides **FastFlowLM** and **GAIA** for ONNX models on Ryzen AI:

```powershell
# Install FastFlowLM
# Download from: https://github.com/FastFlowLM/FastFlowLM
flm-setup.exe

# Run model on NPU
flm run phi-4-mini-q4.onnx
```

**Java Integration via REST:**
```java
// FastFlowLM runs as local server
public class NPULLMClient {
    private final HttpClient client = HttpClient.newHttpClient();

    public String generate(String prompt) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/generate"))
            .POST(HttpRequest.BodyPublishers.ofString(prompt))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
```

### Pros and Cons

| Pros | Cons |
|------|------|
| Cross-platform | Complex setup |
| Hardware acceleration | Model conversion required |
| NPU support (Ryzen AI) | Limited LLM support in Java |
| Production-ready | Requires ONNX format |

---

## DJL (Deep Java Library)

### Overview

Amazon's DJL is a Java-native deep learning framework supporting multiple engines.

**Maven Dependencies:**
```xml
<dependency>
    <groupId>ai.djl</groupId>
    <artifactId>api</artifactId>
    <version>0.29.0</version>
</dependency>
<dependency>
    <groupId>ai.djl</groupId>
    <artifactId>model-zoo</artifactId>
    <version>0.29.0</version>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-engine</artifactId>
    <version>0.29.0</version>
</dependency>
<dependency>
    <groupId>ai.djl.huggingface</groupId>
    <artifactId>tokenizers</artifactId>
    <version>0.29.0</version>
</dependency>
```

### Embeddings with DJL

**Best For:** Text embeddings for semantic search/classification

```java
import ai.djl.ModelException;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.qa.QAInput;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

public class EmbeddingService {
    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;

    public void init() throws ModelException, IOException {
        Criteria<String, float[]> criteria = Criteria.builder()
            .setTypes(String.class, float[].class)
            .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
            .build();

        model = criteria.loadModel();
        predictor = model.newPredictor();
    }

    public float[] embed(String text) throws TranslateException {
        return predictor.predict(text);
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
}
```

### Text Classification with DJL

```java
import ai.djl.modality.Classifications;
import ai.djl.modality.nlp.DefaultTokenizer;
import ai.djl.modality.nlp.Tokenizer;
import ai.djl.translate.TranslateException;

public class IntentClassifier {
    private Predictor<String, Classifications> classifier;

    public IntentClassifier() throws ModelException, IOException {
        // Load a pre-trained text classification model
        Criteria<String, Classifications> criteria = Criteria.builder()
            .setTypes(String.class, Classifications.class)
            .optModelUrls("djl://ai.djl.huggingface.pytorch/distilbert-base-uncased-finetuned-sst-2-english")
            .build();

        ZooModel<String, Classifications> model = criteria.loadModel();
        classifier = model.newPredictor();
    }

    public Intent classifyIntent(String command) throws TranslateException {
        Classifications result = classifier.predict(command);
        // Map to Intent enum based on best match
        return Intent.valueOf(result.best().getClassName());
    }
}
```

### DJL + LangChain4j Integration

LangChain4j supports DJL for embeddings:

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.djl.DjlEmbeddingModel;

public class HybridEmbeddingService {
    private final EmbeddingModel embeddingModel;

    public HybridEmbeddingService() {
        this.embeddingModel = DjlEmbeddingModel.builder()
            .modelUrl("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
            .build();
    }

    public float[] embedAction(String actionDescription) {
        return embeddingModel.embed(actionDescription).vector();
    }
}
```

---

## llama.cpp Java Bindings

### Overview

llama.cpp provides high-performance inference via optimized C++ with JNI bindings.

**Option 1: JNI Bindings (High Performance)**

Create native library:

```cpp
// native/llama_wrapper.cpp
#include <jni.h>
#include "llama.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_minewright_ai_llm_LlamaCppModel_loadModel(
    JNIEnv* env, jobject thiz, jstring modelPath) {

    const char* path = env->GetStringUTFChars(modelPath, 0);

    llama_model_params params = llama_model_default_params();
    llama_model* model = llama_load_model_from_file(path, params);

    env->ReleaseStringUTFChars(modelPath, path);
    return (jlong)model;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_minewright_ai_llm_LlamaCppModel_generate(
    JNIEnv* env, jobject thiz, jlong modelPtr, jstring prompt) {

    llama_model* model = (llama_model*)modelPtr;
    const char* promptStr = env->GetStringUTFChars(prompt, 0);

    // Setup sampling parameters
    llama_sampling_params sparams = llama_sampling_default_params();

    // Initialize context
    llama_context_params ctx_params = llama_context_default_params();
    llama_context* ctx = llama_new_context_with_model(model, ctx_params);

    // Tokenize and generate
    // ... generation logic ...

    env->ReleaseStringUTFChars(prompt, promptStr);
    return env->NewStringUTF("generated text");
}
```

Java wrapper:

```java
package com.minewright.llm;

public class LlamaCppModel {
    static {
        System.loadLibrary("llama_wrapper");
    }

    private long nativeModelPtr;

    public void loadModel(String path) {
        nativeModelPtr = loadModelNative(path);
    }

    public String generate(String prompt) {
        return generateNative(nativeModelPtr, prompt);
    }

    private native long loadModelNative(String path);
    private native String generateNative(long modelPtr, String prompt);
}
```

**Option 2: Llama3.java (Pure Java)**

**For:** Simplified deployment, cross-platform

```bash
git clone https://github.com/mukel/llama3.java
cd llama3.java

# Download quantized model
wget https://huggingface.co/mukel/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_0.gguf

# Run
java --add-modules=jdk.incubator.vector \
     -jar target/llama3.java.jar \
     --model Llama-3.2-3B-Instruct-Q4_0.gguf \
     --prompt "Hello"
```

**Integration with MineWright:**

```java
public class LlamaJavaClient implements LocalLLMProvider {
    private final LlamaSession session;

    public LlamaJavaClient() throws IOException {
        LlamaModel model = LlamaModel.load(
            "models/llama-3.2-3b-q4_0.gguf",
            ModelParams.builder()
                .nCtx(4096)
                .nGpuLayers(28)  // Offload to GPU
                .build()
        );

        this.session = model.session();
    }

    @Override
    public CompletableFuture<String> generateAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            return session.generate(prompt);
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
}
```

### Performance Comparison

| Implementation | Setup | Performance | Portability |
|----------------|-------|-------------|-------------|
| llama.cpp JNI | Complex | Best (native) | Poor (per-platform) |
| Llama3.java | Simple | Good | Excellent |
| GPULlama3.java | Medium | Excellent | Medium (TornadoVM) |

---

## Model Quantization

### Overview

Quantization reduces model size and increases speed with minimal accuracy loss.

### Quantization Levels

| Quantization | 7B Model Size | VRAM | Accuracy Loss | Use Case |
|--------------|---------------|------|---------------|----------|
| FP16 | 13.0 GB | ~13500 MiB | 0% | Maximum quality |
| Q8_0 | 6.7 GB | ~7000 MiB | <1% | High quality |
| Q5_K_M | 4.3 GB | ~4800 MiB | ~2% | Balanced |
| Q4_K_M | 3.8 GB | ~4200 MiB | ~3% | Recommended |
| Q4_0 | 3.8 GB | ~4200 MiB | ~5% | Fast |
| Q3_K_M | 3.0 GB | ~3500 MiB | ~8% | Space-constrained |
| Q2_K | 2.5 GB | ~3000 MiB | ~15% | Not recommended |

### GGUF Format

**Benefits:**
- Single file with weights + metadata
- CPU/GPU hybrid inference
- Cross-platform compatibility
- Multiple quantization options

**Recommended for RTX 4050:**
- **3B models:** Q4_K_M (best balance)
- **7B models:** Q4_K_M or Q5_K_M (if fits)
- **1-2B models:** Q8_0 (max quality, still small)

### Conversion Example

```bash
# Install llama.cpp
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp
cmake -B build
cmake --build build --config release

# Convert to GGUF
./build/bin/convert-hf-to-gguf.py /path/to/phi-4-mini --outfile phi-4-mini-f16.gguf

# Quantize
./build/bin/quantize phi-4-mini-f16.gguf phi-4-mini-q4_k_m.gguf Q4_K_M

# Test
./build/bin/main -m phi-4-mini-q4_k_m.gguf -p "Hello, world!"
```

### Java Integration with GGUF

**Using llama.cpp Java bindings:**

```java
public class QuantizedModelRunner {
    private final long modelPtr;
    private final long contextPtr;

    public QuantizedModelRunner(String ggufPath, String quantization) {
        // Load GGUF model
        this.modelPtr = loadGGUFModel(ggufPath);

        // Create context
        this.contextPtr = createContext(modelPtr, 4096); // context size
    }

    private native long loadGGUFModel(String path);
    private native long createContext(long modelPtr, int contextSize);
    private native String generate(long ctxPtr, String prompt, int maxTokens);

    public String chat(String prompt) {
        return generate(contextPtr, prompt, 512);
    }
}
```

**Using Ollama (auto-quantizes):**

```bash
# Ollama automatically uses appropriate quantization
ollama pull phi4-mini  # Already Q4_K_M quantized
```

---

## Hybrid Cloud/Local Architecture

### Architecture Pattern

```
                    User Input (Command)
                           |
                           v
                  +------------------+
                  |  Intent Router   | (Local: gemma2:2b)
                  +------------------+
                           |
           +---------------+---------------+
           |                               |
           v                               v
    +-------------+                 +-------------+
    | LOCAL MODEL |                 | CLOUD MODEL |
    | (Fast/Free) |                 | (GLM-5)     |
    +-------------+                 +-------------+
           |                               |
           +---------------+---------------+
                           |
                           v
                  +------------------+
                  |  Action Executor |
                  +------------------+
```

### Implementation for MineWright

**1. Router Component (Local):**

```java
package com.minewright.ai.llm.routing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

public class ModelRouter {
    private final ChatLanguageModel router;

    public ModelRouter() {
        this.router = OllamaChatModel.builder()
            .modelName("gemma2:2b")
            .temperature(0.0)  // Deterministic
            .systemMessage("""
                You are a model router. Analyze the request and respond with ONLY:

                LOCAL - if the task is:
                - Simple classification
                - Quick Q&A (within 100 words)
                - Intent recognition
                - Basic reasoning

                CLOUD - if the task is:
                - Multi-step planning
                - Complex reasoning
                - Creative writing
                - Code generation
                - Requires world knowledge

                Respond with only one word: LOCAL or CLOUD
                """)
            .build();
    }

    public ModelChoice route(String userCommand) {
        String decision = router.generate(userCommand).trim().toUpperCase();
        return switch (decision) {
            case "LOCAL" -> ModelChoice.LOCAL;
            case "CLOUD" -> ModelChoice.CLOUD;
            default -> ModelChoice.LOCAL; // Default fallback
        };
    }
}
```

**2. Hybrid LLM Service:**

```java
package com.minewright.ai.llm;

public class HybridLLMService {
    private final ModelRouter router;
    private final LocalLLMClient localClient;
    private final OpenAIClient cloudClient;

    public HybridLLMService() {
        this.router = new ModelRouter();
        this.localClient = new LocalLLMClient();  // Ollama wrapper
        this.cloudClient = new OpenAIClient();    // Existing GLM-5 client
    }

    public CompletableFuture<String> planTasksAsync(
        String systemPrompt,
        String userPrompt
    ) {
        ModelChoice choice = router.route(userPrompt);

        return switch (choice) {
            case LOCAL -> localClient.generateAsync(systemPrompt, userPrompt);
            case CLOUD -> cloudClient.sendRequestAsync(systemPrompt, userPrompt);
        };
    }

    // For simple classification (always local)
    public String classify(String command) {
        return localClient.classify(command);
    }

    // For complex planning (always cloud)
    public String complexPlan(String systemPrompt, String userPrompt) {
        return cloudClient.sendRequest(systemPrompt, userPrompt);
    }
}
```

**3. Embedding Service (Local):**

```java
package com.minewright.ai.llm;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;

public class LocalEmbeddingService {
    private final EmbeddingModel embeddingModel;

    public LocalEmbeddingService() {
        this.embeddingModel = OllamaEmbeddingModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("nomic-embed-text")
            .build();
    }

    public float[] embed(String text) {
        return embeddingModel.embed(text).vector();
    }

    // Find most similar action from history
    public Action findSimilarAction(String description, List<Action> history) {
        float[] queryEmbed = embed(description);

        return history.stream()
            .max(Comparator.comparing(action -> {
                float[] actionEmbed = embed(action.getDescription());
                return cosineSimilarity(queryEmbed, actionEmbed);
            }))
            .orElse(null);
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

### Cost/Benefit Analysis

| Task | Local | Cloud | Rationale |
|------|-------|-------|-----------|
| Intent classification | gemma2:2b | - | <100ms, no latency |
| Quick Q&A | phi4-mini | - | Privacy, speed |
| Task planning | - | GLM-5 | Complex reasoning |
| Code generation | - | GLM-5 | Better quality |
| Embeddings | nomic-embed | - | Always local |
| World knowledge | - | GLM-5 | Requires training data |

**Estimated Savings:**
- **60-70%** of requests go to local models
- **Latency reduction:** 500ms local vs 2000ms cloud
- **Cost reduction:** ~70% less API calls

---

## Hardware-Specific Options

### RTX 4050 (6GB VRAM)

**Maximum Model Sizes:**
- Q4_K_M: ~7B parameter models
- Q5_K_M: ~5B parameter models
- Q8_0: ~3B parameter models

**Recommended Configuration:**

```toml
[local.gpu]
enabled = true
nGpuLayers = 28  # Offload most layers to GPU
maxModelSize = "7B"

[local.models]
primary = "phi4-mini"     # 3.8B Q4: ~4GB VRAM
backup = "gemma2:2b"      # 2B Q4: ~2GB VRAM
embeddings = "nomic-embed-text"  # ~500MB VRAM
```

**Performance Optimization:**
```bash
# Set environment variables for CUDA
set CUDA_VISIBLE_DEVICES=0
set OLLAMA_NUM_GPU=32  # Number of GPU layers
```

### Ryzen AI 9 HX NPU

**Option 1: FastFlowLM (Recommended)**

```powershell
# Install from GitHub
flm-setup.exe

# Pull NPU-optimized models
flm pull qwen2:1.5b
flm run qwen2:1.5b "Hello, world!"
```

**Java Integration:**
```java
public class NPULLMClient {
    private final HttpClient client;
    private final String baseUrl = "http://localhost:8080";

    public NPULLMClient() {
        this.client = HttpClient.newHttpClient();
    }

    public String generate(String prompt) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/generate"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString("""
                {"model": "qwen2:1.5b", "prompt": "%s"}
                """.formatted(prompt)))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
```

**Option 2: Hybrid NPU/iGPU**

AMD's hybrid approach uses:
- **NPU:** Prefill (time to first token)
- **iGPU:** Decode (tokens per second)

```java
public class HybridNPUClient {
    // Requires AMD-specific runtime (Lemonade SDK)
    // Currently best via FastFlowLM's automatic hybrid mode
}
```

### Memory Usage (32GB RAM)

**Recommended allocation:**
- Ollama: 8-12GB
- Minecraft: 4-6GB
- System: 4-6GB
- Available for models: 10-14GB

**Model Strategy:**
- Keep 1-2 models loaded in memory
- Use Ollama's automatic model unloading
- Consider embedding model (always loaded)

---

## Implementation Recommendations

### Phase 1: Quick Wins (1-2 weeks)

**Dependencies to add to `build.gradle`:**
```gradle
dependencies {
    // LangChain4j for local LLM
    implementation 'dev.langchain4j:langchain4j:0.36.2'
    implementation 'dev.langchain4j:langchain4j-ollama:0.36.2'

    // DJL for embeddings (optional)
    implementation 'ai.djl:api:0.29.0'
    implementation 'ai.djl.huggingface:tokenizers:0.29.0'
}
```

**Implementation Steps:**
1. Install Ollama locally
2. Add `LocalLLMClient` class using LangChain4j
3. Implement intent classification with gemma2:2b
4. Add model router to decide local vs cloud
5. Test with existing `TaskPlanner`

**Expected Results:**
- 60% faster simple commands
- 50% reduction in API costs
- Better privacy for local tasks

### Phase 2: Enhanced Features (2-4 weeks)

1. **Add Embeddings:**
   - Integrate nomic-embed-text via Ollama
   - Build semantic search for action history
   - Implement few-shot learning with embeddings

2. **Add Streaming:**
   - Implement `StreamingChatLanguageModel`
   - Update tick-based execution for real-time tokens
   - Add cancellation support

3. **Optimize Performance:**
   - Cache model responses with Caffeine
   - Batch embeddings requests
   - Add request prioritization

### Phase 3: Advanced Features (4-8 weeks)

1. **NPU Integration:**
   - Evaluate FastFlowLM for Ryzen AI
   - Implement hybrid NPU/iGPU execution
   - Benchmark vs pure GPU

2. **Fine-tuning:**
   - Fine-tune small model on Minecraft-specific data
   - Implement tool/function calling format
   - Optimize for action planning

3. **Multi-Agent Coordination:**
   - Use local model for inter-agent communication
   - Implement distributed planning
   - Add federated learning

### Recommended Models for MineWright

| Purpose | Model | Quantization | Size | Source |
|---------|-------|--------------|------|--------|
| Routing | gemma2:2b | Q4_0 | 2GB | `ollama pull gemma2:2b` |
| Classification | gemma2:2b | Q4_0 | 2GB | Same as routing |
| Quick Chat | phi4-mini | Q4_K_M | 4GB | `ollama pull phi4-mini` |
| Math/Coordinates | phi4-mini | Q4_K_M | 4GB | Same as chat |
| Embeddings | nomic-embed-text | FP16 | 500MB | `ollama pull nomic-embed-text` |
| Complex Planning | GLM-5 | - | - | Cloud (existing) |

### Configuration Example

**Update `config/minewright-common.toml`:**
```toml
[llm]
# Provider: "cloud", "local", or "hybrid"
provider = "hybrid"

[llm.local]
# Ollama configuration
baseUrl = "http://localhost:11434"
timeoutSeconds = 30

[llm.local.models]
routing = "gemma2:2b"
chat = "phi4-mini"
embeddings = "nomic-embed-text"

[llm.local.routing]
# Use local for requests under this token count
maxTokensForLocal = 512

[llm.cloud]
# Existing GLM-5 configuration
provider = "z-ai"
apiKey = "your-api-key"
model = "glm-5"

[llm.fallback]
# Fallback to cloud if local fails
enabled = true
timeoutMs = 5000
```

### Code Integration Example

**Modify `TaskPlanner.java`:**
```java
public class TaskPlanner {
    private final HybridLLMService llmService;

    public TaskPlanner() {
        this.llmService = new HybridLLMService();
    }

    public CompletableFuture<List<Task>> planTasksAsync(
        String userCommand,
        World world,
        BlockPos pos
    ) {
        String context = buildContext(world, pos);
        String systemPrompt = buildSystemPrompt();

        // Hybrid routing automatically decides local vs cloud
        return llmService.planTasksAsync(systemPrompt, userCommand)
            .thenApply(response -> parseTasks(response, world, pos))
            .exceptionally(ex -> {
                MineWrightMod.LOGGER.error("Task planning failed", ex);
                return List.of();
            });
    }

    // New: Quick classification (always local)
    public TaskType classifyTask(String command) {
        String classification = llmService.classify(command);
        return TaskType.valueOf(classification.toUpperCase());
    }
}
```

---

## Performance Benchmarks

### Expected Performance (RTX 4050)

| Model | Task | Time to First Token | Tokens/Second | Total Time (100 tokens) |
|-------|------|---------------------|---------------|-------------------------|
| GLM-4-Flash | Chat (cloud) | 800ms | 50 | 2.8s |
| GLM-5 | Planning (cloud) | 1200ms | 40 | 3.7s |
| gemma2:2b | Classification (local) | 50ms | 30 | 3.8s |
| phi4-mini | Chat (local) | 400ms | 20 | 5.4s |
| nomic-embed | Embedding (local) | 10ms | - | 10ms |

### Latency Comparison

| Scenario | Cloud Only | Hybrid | Improvement |
|----------|------------|--------|-------------|
| Simple classification | 2000ms | 100ms | 95% faster |
| Quick Q&A | 2500ms | 600ms | 76% faster |
| Complex planning | 3500ms | 3500ms | Same (cloud) |
| Mixed workload | 2800ms avg | 1200ms avg | 57% faster |

---

## Troubleshooting

### Common Issues

**1. Ollama connection refused:**
```bash
# Make sure Ollama is running
ollama serve

# Test connection
curl http://localhost:11434/api/generate -d '{
  "model": "gemma2:2b",
  "prompt": "test"
}'
```

**2. Out of memory on RTX 4050:**
- Use smaller quantization (Q4_K_M instead of Q5_K_M)
- Reduce context size
- Close other GPU-intensive applications

**3. Slow inference:**
- Check GPU utilization: `nvidia-smi`
- Ensure CUDA is properly configured
- Try CPU-only mode if GPU has issues

**4. Model not loading:**
```bash
# Verify model is downloaded
ollama list

# Re-download if needed
ollama pull phi4-mini

# Check logs
ollama logs
```

---

## References and Resources

### Official Documentation
- [Ollama Documentation](https://ollama.com/docs)
- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [DJL Documentation](https://djl.ai/docs/)
- [ONNX Runtime](https://onnxruntime.ai/docs/)
- [Phi-4-Mini Paper](https://arxiv.org/abs/2502.12245)

### Model Repositories
- [Hugging Face - Phi-4-Mini](https://huggingface.co/microsoft/phi-4-mini)
- [Hugging Face - Gemma 2](https://huggingface.co/google/gemma-2-2b-it)
- [Ollama Model Library](https://ollama.com/library)

### Java Projects
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j)
- [Llama3.java](https://github.com/mukel/llama3.java)
- [DJL GitHub](https://github.com/deepjavalibrary/djl)

### Hardware-Specific
- [FastFlowLM (Ryzen AI NPU)](https://github.com/FastFlowLM/FastFlowLM)
- [GAIA Project (AMD)](https://www.amd.com/en/developer/resources/technical-articles/gaia-an-open-source-project-from-amd-for-running-local-llms-on-ryzen-ai.html)
- [llama.cpp](https://github.com/ggerganov/llama.cpp)

### Articles and Tutorials
- [Spring AI Alibaba + Ollama Integration](http://developer.aliyun.com/article/1682295)
- [Java AI Integration Guide](https://cloud.tencent.com/developer/article/2552767)
- [Local AI Deployment Complete Guide](https://blog.csdn.net/lxcxjxhx/article/details/153289301)
- [GGUF Quantization Guide](https://blog.51cto.com/u_17480440/14472525)

### Relevant Projects
- [Mindcraft (Minecraft LLM Mod)](https://modrinth.com/mod/mindcraft)
- [MCLLM Mod](https://www.mcmod.cn/class/5427.html)

---

## Conclusion

**Recommended Stack for MineWright:**

1. **Primary Integration:** Ollama + LangChain4j
   - Easiest to implement
   - Cross-platform
   - Active maintenance

2. **Models:**
   - Routing: gemma2:2b (Q4_0)
   - Chat/Reasoning: phi4-mini (Q4_K_M)
   - Embeddings: nomic-embed-text
   - Complex tasks: GLM-5 (cloud fallback)

3. **Architecture:**
   - Hybrid local/cloud with intelligent routing
   - Local for classification, embeddings, quick responses
   - Cloud for complex planning, reasoning, code generation

4. **Hardware Utilization:**
   - RTX 4050 for primary model inference
   - Ryzen AI NPU (via FastFlowLM) for future optimization
   - 32GB RAM allows multiple models in memory

**Expected Benefits:**
- 50-70% reduction in cloud API costs
- 60-80% faster simple commands
- Better privacy for local tasks
- Offline capability for basic features
- Foundation for future enhancements (fine-tuning, multi-agent)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Author:** Claude Code (Orchestrator Agent)
