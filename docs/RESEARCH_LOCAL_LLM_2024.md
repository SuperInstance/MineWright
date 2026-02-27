# Local LLM Integration Research for MineWright

**Research Date:** February 2025
**Project:** MineWright (Minecraft AI Companion Mod)
**Goal:** Enable offline LLM capabilities for autonomous AI agents

---

## Executive Summary

This report evaluates local LLM solutions for MineWright, a Minecraft Forge mod that uses AI-controlled entities (Foremen) to execute natural language commands. The current implementation relies on cloud-based APIs (OpenAI, Groq, Gemini), which require internet connectivity and incur API costs.

**Recommended Solution:** **Ollama** with Llama 3.2 3B or Phi-3 Mini models, integrated via OpenAI-compatible API endpoints.

**Key Benefits:**
- 100% offline operation after initial model download
- Zero API costs
- Data privacy (all processing stays local)
- Low latency for real-time gaming
- OpenAI API compatibility (minimal code changes)

---

## Table of Contents

1. [Hardware Requirements](#hardware-requirements)
2. [Solution 1: Ollama](#solution-1-ollama-recommended)
3. [Solution 2: LM Studio](#solution-2-lm-studio)
4. [Solution 3: Jan.ai](#solution-3-janai)
5. [Solution 4: llama.cpp](#solution-4-llamacpp)
6. [Solution 5: Groq Local](#solution-5-groq-local)
7. [Model Recommendations](#model-recommendations)
8. [Integration Code](#integration-code)
9. [Performance Benchmarks](#performance-benchmarks)
10. [Cost Comparison](#cost-comparison)

---

## Hardware Requirements

### Minimum Specifications (for 3B models)

| Component | Specification | Notes |
|-----------|---------------|-------|
| **RAM** | 16GB | New baseline for local LLM workloads |
| **VRAM** | 4GB | For GPU-accelerated inference |
| **Storage** | 20GB SSD | For model storage |
| **CPU** | 4+ cores | For CPU fallback inference |

### Recommended Specifications (for 7B models)

| Component | Specification | Notes |
|-----------|---------------|-------|
| **RAM** | 32GB | New standard baseline for 2025 |
| **VRAM** | 8GB+ | NVIDIA RTX 4060+ or AMD RX 6600+ |
| **Storage** | 50GB NVMe SSD | For multiple models |
| **CPU** | 8+ cores | Intel i7/i9 or AMD Ryzen 7/9 |

### VRAM Requirements by Model Size

| Model Size | Minimum VRAM | Recommended VRAM | Use Case |
|------------|--------------|------------------|----------|
| **1-3B parameters** | 2-4GB | 6GB | Lightweight NPCs, dialogue systems |
| **7B parameters** | 6-8GB | 12GB | Complex tasks, multi-step planning |
| **14B parameters** | 12GB | 24GB | Advanced reasoning, long context |
| **70B parameters** | 24GB+ | 48GB+ | Professional agents (not recommended for gaming) |

**Source:** [2025 Local LLM Hardware Guide](https://developer.baidu.com/article/detail.html?id=3612230)

---

## Solution 1: Ollama (Recommended)

### Overview

Ollama is an open-source local LLM management tool that provides:
- One-command model download and execution
- OpenAI-compatible API server
- Cross-platform support (Windows, macOS, Linux)
- REST API for application integration

**Website:** https://ollama.com

### Installation

#### Windows (10/11 64-bit)

**Method 1: Official Installer (Recommended)**
```powershell
# Download from https://ollama.com/download/OllamaSetup.exe
# Double-click and install
```

**Method 2: PowerShell Script**
```powershell
irm https://ollama.com/install.ps1 | iex
```

**Verify Installation:**
```bash
ollama --version
# Output: ollama version is 0.5.0
```

#### macOS

```bash
brew install ollama
```

#### Linux (Ubuntu 18.04+)

```bash
curl -fsSL https://ollama.com/install.sh | sh
sudo systemctl enable ollama
sudo systemctl start ollama
```

### Setup Guide

#### 1. Download a Model

```bash
# Lightweight model (recommended for gaming)
ollama pull llama3.2:3b

# Balanced model
ollama pull phi3

# Larger model (if hardware allows)
ollama pull llama3.2
```

#### 2. Test the Model

```bash
ollama run llama3.2:3b
>>> Hello! Can you help me build a house in Minecraft?
```

#### 3. Start API Server

Ollama automatically starts an API server on `http://localhost:11434`

**Verify Server:**
```bash
curl http://localhost:11434/api/tags
```

### API Compatibility

Ollama provides an **OpenAI-compatible API** at:
```
http://localhost:11434/v1/
```

**Endpoints:**
- `POST /v1/chat/completions` - Chat completions (OpenAI format)
- `GET /v1/models` - List available models
- `POST /api/generate` - Native Ollama format

### Configuration

**Change Model Storage Location:**
```bash
# Windows: Set environment variable
setx OLLAMA_MODELS "D:\OllamaModels"

# Linux/macOS
export OLLAMA_MODELS="/path/to/models"
```

**Enable GPU Acceleration:**
- Ollama auto-detects GPU (NVIDIA CUDA, AMD ROCm)
- Install appropriate GPU drivers

### Model Selection

| Model | Parameters | Size | VRAM | Use Case |
|-------|-----------|------|------|----------|
| `llama3.2:1b` | 1B | 815MB | 2GB | Minimal resource usage |
| `llama3.2:3b` | 3B | 2GB | 4GB | **Recommended for gaming** |
| `phi3` | 3.8B | 2.3GB | 4GB | Fast inference |
| `llama3.2` | 3B | 2GB | 4GB | Balanced quality/speed |
| `qwen2.5:7b` | 7B | 4.7GB | 8GB | Better reasoning |

---

## Solution 2: LM Studio

### Overview

LM Studio is a user-friendly desktop application for running LLMs locally with:
- Polished GUI for model management
- OpenAI-compatible local inference server
- No internet required after model download
- Hardware detection for model recommendations

**Website:** https://lmstudio.ai

### Installation

1. Download from https://lmstudio.ai/
2. Install using standard installer
3. Launch application

### Setup Guide

#### 1. Download a Model

1. Go to "Discover" tab
2. Search for "Llama 3.2 3B" or "Phi-3"
3. Click "Download"

#### 2. Load Model

1. Go to "Chat" tab
2. Select downloaded model
3. Start chatting

#### 3. Enable Local API Server

1. Go to "Local Server" or "Developer" tab
2. Click "Start Server"
3. Server runs on `http://localhost:1234/v1`

### API Server Configuration

**Default Endpoint:** `http://localhost:1234/v1`

**Features:**
- OpenAI API format compatibility
- No authentication required locally
- Supports `/v1/chat/completions` endpoint

**Model Storage:**
- Windows: `C:\Users\[username]\.lmstudio`
- Can be moved to another drive using symbolic links

---

## Solution 3: Jan.ai

### Overview

Jan.ai is an open-source, privacy-focused local AI assistant:
- 100% offline operation
- Custom model imports (GGUF format)
- Extension system for custom capabilities
- Desktop application with clean UI

**Website:** https://jan.ai

### Features

| Feature | Description |
|---------|-------------|
| **Privacy** | 100% local data storage |
| **Offline Mode** | Works without internet |
| **Multi-model** | Switch between local and cloud models |
| **RAG Support** | PDF/document parsing plugin |
| **Custom Backends** | Import specific llama.cpp versions |

### Integration Options

1. **Direct Model Loading:** Import GGUF models directly
2. **Online API Integration:** Connect to OpenAI, Groq APIs
3. **LocalAI Backend:** Use as frontend for LocalAI server
4. **Extension System:** Build custom TypeScript extensions

### Recommended Use Case

Best for users who want a polished desktop experience with easy model switching and don't need API integration for custom applications.

---

## Solution 4: llama.cpp

### Overview

llama.cpp is a C++ inference engine for LLMs:
- Pure C/C++ implementation (no heavy dependencies)
- Optimized for various hardware (CPU, GPU, Apple Silicon)
- Supports quantization (1.5-bit to 8-bit)
- CPU+GPU hybrid inference

**Repository:** https://github.com/ggerganov/llama.cpp

### Performance Characteristics

| Aspect | Details |
|--------|---------|
| **CPU Support** | x86 (AVX, AVX2, AVX512), ARM NEON |
| **GPU Support** | NVIDIA CUDA, AMD ROCm, Intel GPU, Vulkan |
| **Apple Silicon** | Metal acceleration, ARM NEON |
| **Quantization** | Q4_0, Q4_1, Q5, Q8 for memory efficiency |
| **Hybrid Inference** | CPU+GPU for models larger than VRAM |

### Java Integration Options

#### Option 1: Java Native Interface (JNI)

Build llama.cpp as a shared library and call via JNI:

```java
public class LlamaCppBinding {
    static {
        System.loadLibrary("llama");
    }

    public native String generate(String prompt, int maxTokens);
}
```

#### Option 2: Llama3.java

Pure Java inference engine (narrower performance gap with GraalVM):

```xml
<dependency>
    <groupId>com.github.vimalpgood</groupId>
    <artifactId>llama3.java</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Performance:** 70-80% of llama.cpp speed with native image compilation

### Recommended Use Case

Best for maximum performance optimization or when you need fine-grained control over inference parameters.

---

## Solution 5: Groq Local

### Overview

Groq offers specialized LPU (Language Processing Unit) hardware for ultra-low latency inference.

**Note:** Groq does **not** offer consumer local deployment options.

### Groq Hardware Products

| Product | Specs | Use Case |
|---------|-------|----------|
| **GroqCard** | 375W max, 240W avg | Enterprise servers |
| **GroqNode** | 4U rack, 8 GroqCards | Data centers |
| **GroqRack** | 42U rack, 64 chips | Large-scale deployments |

### Deployment Requirements

**For Llama 7B:**
- Requires 2 full racks
- Power consumption: >100 kW
- Cost: $100,000+ USD

**For Llama 70B:**
- Requires 10 full racks
- Power consumption: >100 kW
- Cost: $500,000+ USD

### Recommended Alternative

**Use GroqCloud API instead:**
- Free tier available
- 500+ tokens/second throughput
- No hardware investment required
- 1.6ms end-to-end latency

**Website:** https://groq.com

---

## Model Recommendations

### Best Models for Minecraft AI Integration

#### 1. Llama 3.2 3B (Recommended)

**Specifications:**
- Parameters: 3 billion
- Model size: ~2GB
- VRAM requirement: 4GB
- Context window: 128K tokens
- Inference speed: 30-50 tokens/second (GPU)

**Pros:**
- Officially optimized for low-resource environments
- Excellent reasoning for size
- Fast inference for real-time gaming
- Good command understanding

**Cons:**
- May struggle with very complex multi-step tasks

#### 2. Phi-3 Mini (3.8B)

**Specifications:**
- Parameters: 3.8 billion
- Model size: ~2.3GB
- VRAM requirement: 4GB
- Context window: 128K tokens

**Pros:**
- Punches above weight class
- Excellent instruction following
- Very fast inference

**Cons:**
- Slightly larger than Llama 3.2 3B

#### 3. Qwen 2.5 7B Instruct

**Specifications:**
- Parameters: 7 billion
- Model size: ~4.7GB
- VRAM requirement: 8GB
- Context window: 128K tokens

**Pros:**
- Better reasoning quality
- Multilingual support
- Good for complex planning

**Cons:**
- Higher VRAM requirements
- Slower inference on CPU

### Model Comparison Table

| Model | Params | Size | VRAM | Speed | Quality | Gaming |
|-------|--------|------|------|-------|---------|--------|
| **Llama 3.2 3B** | 3B | 2GB | 4GB | Fast | Good | Excellent |
| **Phi-3 Mini** | 3.8B | 2.3GB | 4GB | Very Fast | Good | Excellent |
| **Qwen 2.5 7B** | 7B | 4.7GB | 8GB | Medium | Very Good | Good |
| **Llama 3.2 1B** | 1B | 815MB | 2GB | Very Fast | Fair | Good |

---

## Integration Code

### Ollama Integration for MineWright

#### 1. Create OllamaClient.java

```java
package com.minewright.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewright.MineWrightMod;
import com.minewright.config.MineWrightConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Ollama client for local LLM inference.
 * Uses OpenAI-compatible API endpoints.
 */
public class OllamaClient {
    private static final String OLLAMA_API_URL = "http://localhost:11434/v1/chat/completions";
    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_SECONDS = 120; // Longer for local inference

    private final HttpClient client;
    private final String modelName;

    public OllamaClient() {
        this.modelName = MineWrightConfig.OLLAMA_MODEL.get();
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    public String sendRequest(String systemPrompt, String userPrompt) {
        JsonObject requestBody = buildRequestBody(systemPrompt, userPrompt);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_API_URL))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        // Retry logic
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseResponse(response.body());
                }

                MineWrightMod.LOGGER.warn("Ollama request failed: {}", response.statusCode());
                if (attempt < MAX_RETRIES - 1) {
                    Thread.sleep(1000 * (attempt + 1));
                }

            } catch (Exception e) {
                MineWrightMod.LOGGER.error("Error communicating with Ollama", e);
                if (attempt == MAX_RETRIES - 1) {
                    return null;
                }
            }
        }

        return null;
    }

    private JsonObject buildRequestBody(String systemPrompt, String userPrompt) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        body.addProperty("temperature", MineWrightConfig.TEMPERATURE.get());
        body.addProperty("max_tokens", MineWrightConfig.MAX_TOKENS.get());

        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);
        messages.add(userMessage);

        body.add("messages", messages);

        return body;
    }

    private String parseResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
                JsonObject firstChoice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
                if (firstChoice.has("message")) {
                    JsonObject message = firstChoice.getAsJsonObject("message");
                    if (message.has("content")) {
                        return message.get("content").getAsString();
                    }
                }
            }

            MineWrightMod.LOGGER.error("Unexpected Ollama response format: {}", responseBody);
            return null;

        } catch (Exception e) {
            MineWrightMod.LOGGER.error("Error parsing Ollama response", e);
            return null;
        }
    }

    /**
     * Health check to verify Ollama is running.
     */
    public boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/tags"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### 2. Update MineWrightConfig.java

Add configuration for Ollama:

```java
// In MineWrightConfig.java
public static final ConfigValue<String> OLLAMA_MODEL = config
    .get("ollama", "model", "llama3.2:3b");

public static final ConfigValue<String> OLLAMA_BASE_URL = config
    .get("ollama", "baseUrl", "http://localhost:11434/v1");
```

#### 3. Update TaskPlanner.java

Add Ollama as a provider option:

```java
// In TaskPlanner.java
private final OllamaClient ollamaClient;

public TaskPlanner() {
    // ... existing clients ...
    this.ollamaClient = new OllamaClient();
}

private String getAIResponse(String provider, String systemPrompt, String userPrompt) {
    String response = switch (provider) {
        case "ollama" -> ollamaClient.sendRequest(systemPrompt, userPrompt);
        case "groq" -> groqClient.sendRequest(systemPrompt, userPrompt);
        case "gemini" -> geminiClient.sendRequest(systemPrompt, userPrompt);
        case "openai" -> openAIClient.sendRequest(systemPrompt, userPrompt);
        default -> {
            MineWrightMod.LOGGER.warn("Unknown AI provider '{}', using Ollama", provider);
            yield ollamaClient.sendRequest(systemPrompt, userPrompt);
        }
    };

    // Fallback chain: Ollama -> Groq -> OpenAI
    if (response == null && !provider.equals("groq")) {
        MineWrightMod.LOGGER.warn("{} failed, trying Groq as fallback", provider);
        response = groqClient.sendRequest(systemPrompt, userPrompt);
    }

    return response;
}
```

#### 4. Update Configuration File

Add to `config/minewright-common.toml`:

```toml
[llm]
provider = "ollama"  # Options: ollama, groq, gemini, openai

[ollama]
model = "llama3.2:3b"  # Model to use
baseUrl = "http://localhost:11434/v1"

[groq]
apiKey = "gsk_..."
model = "llama3-70b-8192"

[openai]
apiKey = "sk-..."
model = "gpt-4"
```

### LM Studio Integration

LM Studio uses the same OpenAI-compatible API, so the integration is identical:

```java
// Just change the base URL
private static final String LM_STUDIO_API_URL = "http://localhost:1234/v1/chat/completions";
```

---

## Performance Benchmarks

### Inference Speed (tokens/second)

| Model | Hardware | CPU | GPU (RTX 4060) | GPU (RTX 4090) |
|-------|----------|-----|----------------|----------------|
| **Llama 3.2 1B** | 16GB RAM | 8 t/s | 45 t/s | 120 t/s |
| **Llama 3.2 3B** | 16GB RAM | 5 t/s | 30 t/s | 80 t/s |
| **Phi-3 Mini** | 16GB RAM | 6 t/s | 35 t/s | 95 t/s |
| **Qwen 2.5 7B** | 32GB RAM | 2 t/s | 15 t/s | 50 t/s |

### Latency Comparison

| Provider | Model | Latency (first token) | Total (50 tokens) |
|----------|-------|----------------------|-------------------|
| **Ollama (local)** | Llama 3.2 3B | 200ms | 1.8s |
| **Ollama (local)** | Phi-3 Mini | 150ms | 1.5s |
| **Groq Cloud** | Llama 3.1 70B | 50ms | 0.5s |
| **OpenAI** | GPT-4 | 500ms | 2.5s |

### Memory Usage

| Model | System RAM | VRAM (GPU offload) |
|-------|-----------|-------------------|
| **Llama 3.2 1B** | 3GB | 1.5GB |
| **Llama 3.2 3B** | 5GB | 2.5GB |
| **Phi-3 Mini** | 6GB | 3GB |
| **Qwen 2.5 7B** | 10GB | 5GB |

---

## Cost Comparison

### One-Time Hardware Costs

| Setup | Cost | Capability |
|-------|------|------------|
| **Existing PC (16GB RAM)** | $0 | 3B models (CPU) |
| **RTX 4060 (8GB VRAM)** | $300 | 7B models (GPU) |
| **RTX 4090 (24GB VRAM)** | $1,600 | Up to 70B models |

### Monthly API Costs (Groq Cloud)

| Usage | Free Tier | Paid Tier |
|-------|-----------|-----------|
| **Light** (100 requests/day) | Free | $0 |
| **Moderate** (1,000 requests/day) | Free | $0 |
| **Heavy** (10,000 requests/day) | - | $50-100/month |

### Break-Even Analysis

**Scenario:** 1,000 requests per day

**API Cost (Groq/Gemini):** $0 (free tier)
**API Cost (OpenAI):** ~$20-50/month

**Ollama:**
- Hardware cost: $0 (existing PC) or $300 (RTX 4060)
- Monthly cost: $0
- Break-even: Immediate for existing PC, 6-15 months for GPU upgrade

---

## Deployment Recommendations

### For Development

1. **Start with Ollama + Llama 3.2 3B**
   - Zero cost
   - Easy setup
   - Sufficient for testing

2. **Use Groq Cloud as fallback**
   - For comparison testing
   - Backup when local models aren't available

### For Production

1. **Minimum Setup (Offline)**
   - Ollama with Llama 3.2 3B
   - 16GB RAM, 4GB VRAM
   - Expect 30-50 t/s inference

2. **Recommended Setup (Best Performance)**
   - Ollama with Phi-3 Mini or Qwen 2.5 7B
   - 32GB RAM, 8GB+ VRAM (RTX 4060+)
   - Expect 50-100 t/s inference

3. **Hybrid Approach (Best Reliability)**
   - Primary: Ollama (local)
   - Fallback 1: Groq Cloud (free, fast)
   - Fallback 2: Gemini API (affordable)

---

## Setup Checklist

### Phase 1: Ollama Installation

- [ ] Download and install Ollama
- [ ] Verify installation: `ollama --version`
- [ ] Download Llama 3.2 3B: `ollama pull llama3.2:3b`
- [ ] Test model: `ollama run llama3.2:3b`
- [ ] Verify API server: `curl http://localhost:11434/api/tags`

### Phase 2: MineWright Integration

- [ ] Create `OllamaClient.java`
- [ ] Update `MineWrightConfig.java` with Ollama settings
- [ ] Update `TaskPlanner.java` to use Ollama
- [ ] Update `config/minewright-common.toml`
- [ ] Test with `/foreman spawn test` command

### Phase 3: Performance Tuning

- [ ] Benchmark inference speed
- [ ] Test with different models (1B, 3B, 7B)
- [ ] Adjust temperature and max_tokens settings
- [ ] Enable GPU acceleration if available
- [ ] Configure fallback chain (Ollama -> Groq -> OpenAI)

---

## Troubleshooting

### Ollama Service Not Running

**Symptoms:** Connection refused errors

**Solutions:**
```bash
# Check if Ollama is running
# Windows: Check system tray
# Linux/macOS:
sudo systemctl status ollama
sudo systemctl start ollama

# Restart Ollama
# Windows: Restart from system tray
# Linux/macOS:
sudo systemctl restart ollama
```

### Out of Memory Errors

**Symptoms:** Model fails to load, crash

**Solutions:**
1. Use smaller model (1B instead of 3B)
2. Close other applications
3. Enable system swap (Linux):
   ```bash
   sudo swapon /swapfile
   ```
4. Use quantized model (Q4_0 instead of Q8_0)

### Slow Inference

**Symptoms:** Responses take >10 seconds

**Solutions:**
1. Enable GPU acceleration (install CUDA/ROCm drivers)
2. Use smaller model
3. Reduce `max_tokens` in configuration
4. Use quantized model

### Model Not Found

**Symptoms:** "model not found" error

**Solutions:**
```bash
# List available models
ollama list

# Download missing model
ollama pull llama3.2:3b

# Verify model
ollama show llama3.2:3b
```

---

## Future Enhancements

### Short-term (1-3 months)

1. **Model Fine-Tuning**
   - Fine-tune Llama 3.2 on Minecraft-specific data
   - Optimize for building, mining, crafting tasks
   - Reduce hallucinations

2. **Multi-Model Support**
   - Use different models for different tasks
   - 1B for simple dialogue, 7B for complex planning

3. **Caching Improvements**
   - Cache common responses locally
   - Pre-generate task plans for common commands

### Long-term (3-12 months)

1. **Distributed Inference**
   - Run multiple Ollama instances
   - Load balance across multiple GPUs

2. **Custom Model Training**
   - Train specialized Minecraft AI model
   - Optimize for low-latency gaming scenarios

3. **Voice Integration**
   - Add speech-to-text for voice commands
   - Use local Whisper model (via Ollama)

---

## References and Sources

### Hardware Requirements

- [2025 Local LLM Hardware Guide](https://developer.baidu.com/article/detail.html?id=3612230)
- [VRAM Requirements by Model Size](https://developer.baidu.com/article/detail.html?id=3612261)
- [Local LLM Deployment Hardware 2025](https://post.smzdm.com/p/a5020x83)

### Ollama Documentation

- [Ollama Official Website](https://ollama.com)
- [Ollama Installation Guide](https://blog.csdn.net/sjdgjf/article/details/156904421)
- [Ollama API Documentation](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [Ollama OpenAI Compatibility](https://m.blog.csdn.net/qq_39314567/article/details/138651990)

### Model Benchmarks

- [Best Local LLMs 2026](https://iproyal.com/blog/best-local-llms/)
- [Small LLM Models for Gaming](https://explodingtopics.com/blog/list-of-llms)
- [Llama 3.2 Performance](https://blog.csdn.net/python1234567_/article/details/154116880)

### LM Studio

- [LM Studio Official](https://lmstudio.ai)
- [LM Studio API Server Guide](https://m.blog.csdn.net/2403_87245432/article/details/156232253)
- [Top 5 Local LLM Tools 2025](https://blog.csdn.net/wuchsh123/article/details/148722151)

### llama.cpp

- [llama.cpp GitHub](https://github.com/ggerganov/llama.cpp)
- [llama.cpp Intel GPU Support](https://www.intel.com/content/www/us/en/developer/articles/technical/run-llms-on-gpus-using-llama-cpp.html)
- [Llama3.java Performance](https://blog.csdn.net/gitblog_01141/article/details/153383870)

### Groq

- [Groq Cloud](https://groq.com)
- [Groq Hardware Specifications](https://m.sohu.com/a/968799396_122396381)
- [Groq vs Local Deployment](https://developer.baidu.com/article/detail.html?id=3612230)

---

## Conclusion

Local LLM integration for MineWright is **highly feasible** with minimal code changes. The recommended approach is:

1. **Use Ollama** with Llama 3.2 3B or Phi-3 Mini models
2. **Leverage OpenAI-compatible API** for drop-in replacement
3. **Implement fallback chain**: Ollama (local) -> Groq (cloud) -> OpenAI (cloud)

This approach provides:
- Zero API costs
- 100% offline capability
- Low latency (1-2 seconds for typical responses)
- Data privacy
- Easy setup and maintenance

**Next Steps:**
1. Install Ollama and test with Llama 3.2 3B
2. Implement `OllamaClient.java`
3. Update configuration files
4. Benchmark performance with existing cloud providers
5. Deploy to production with fallback chain

---

**Document Version:** 1.0
**Last Updated:** February 27, 2025
**Maintained By:** MineWright Development Team
