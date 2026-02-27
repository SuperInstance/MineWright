# NPU Integration - Quick Reference Guide

## TL;DR

**Can we use the Ryzen AI NPU directly from Java?** NO - No Java SDK exists.

**What should we do instead?**
1. Use RTX 4050 (CUDA) for local LLM inference
2. Use ONNX Runtime for local embeddings
3. Monitor AMD for future Java SDK releases

**Expected Benefits:**
- 5-10x faster task planning (200-500ms vs 1-3s)
- 90% cost reduction (fewer API calls)
- Offline capability
- Better privacy

---

## Recommended Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     MineWright Mod                             │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  Press K → TaskPlanner → Choose Provider:                    │
│                                                               │
│  ┌──────────┐  ┌────────────┐  ┌─────────┐  ┌──────────┐   │
│  │  Local   │  │   Local    │  │  Cloud  │  │   CPU    │   │
│  │   GPU    │  │ Embeddings │  │   LLM   │  │ Fallback │   │
│  │ (RTX4050)│  │ (ONNX RT)  │  │(OpenAI) │  │          │   │
│  └──────────┘  └────────────┘  └─────────┘  └──────────┘   │
│      ▼               ▲
│      │               │
│      └───────┬───────┘
│              ▼
│      ForemanMemory (Semantic Search)
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## Implementation Steps

### Phase 1: Local Embeddings (Week 1-2)

**Objective:** Fast semantic search for memory retrieval

**Model:** `all-MiniLM-L6-v2.onnx` (80MB)

```bash
# Download model
wget https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/model.onnx \
  -O models/all-MiniLM-L6-v2.onnx

# Add to build.gradle
implementation 'com.microsoft.onnxruntime:onnxruntime:1.17.0'
```

**Code:**
```java
// Initialize
LocalEmbeddingModel embeddings = new LocalEmbeddingModel("models/all-MiniLM-L6-v2.onnx");

// Generate embedding
float[] embedding = embeddings.generateEmbedding("minewright mined diamond");

// Semantic search
double similarity = embeddings.cosineSimilarity(
    embedding1,
    embedding2
);
```

**Expected Performance:**
- CPU: 200-500ms per embedding
- GPU (CUDA): 50-100ms per embedding

---

### Phase 2: Local LLM (Week 3-4)

**Objective:** Run task planning locally without cloud APIs

**Model:** `mistral-7b-instruct-v0.2.Q4_K_M.gguf` (4.3GB, fits in 6GB VRAM)

```bash
# Download model
wget https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf \
  -O models/mistral-7b-instruct-v0.2.Q4_K_M.gguf

# Add to build.gradle
implementation 'com.github.devoxygen:java-llama.cpp:1.0.0'
```

**Code:**
```java
// Initialize (offloads to RTX 4050)
LocalLLMProvider llm = new LocalLLMProvider();

// Run inference
LLMResponse response = llm.sendAsync(
    "Generate a plan to build a house",
    Map.of("maxTokens", 1000, "temperature", 0.7)
).join();
```

**Expected Performance:**
- Token generation: 30-50 tokens/second
- First token latency: ~200ms
- VRAM usage: ~5GB

---

## Configuration

```toml
# config/minewright-common.toml

[llm]
# Primary: Local GPU (fastest)
provider = "local-gpu"
# Fallback: Cloud API
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

## Performance Comparison

| Task | Cloud API | Local GPU (RTX 4050) | Local CPU |
|------|-----------|----------------------|-----------|
| 7B LLM Inference | 500-2000ms | 200-500ms | 5000ms+ |
| Embedding Generation | 200-500ms | 50-100ms | 200-500ms |
| Memory Search (10k) | 100-300ms | 20-50ms | 100-300ms |
| Task Planning | 1-3s | 300ms-1s | 5-10s |

---

## Power Consumption

| Configuration | Idle | Load |
|---------------|------|------|
| Cloud API only | 5W | 10W (network) |
| RTX 4050 inference | 15W | 50-80W |
| NPU inference | 5W | 10-15W |
| CPU inference | 10W | 30-50W |

---

## Key Files Created

1. **NPU_INTEGRATION.md** - Full research document
2. **EMBEDDING_SERVICE_EXAMPLE.java** - Embedding model implementation
3. **ENHANCED_STEVE_MEMORY.java** - Semantic search memory system
4. **BUILD_CONFIG_GRADLE.gradle** - Build configuration with dependencies

---

## What About NPU?

### Current Status: NOT FEASIBLE

**Problem:**
- AMD Ryzen AI SDK has NO Java support
- Only Python and C++ APIs available
- No JNI wrappers or official Java bindings

**Future:**
- Monitor [AMD Ryzen AI Developer Portal](https://www.amd.com/en/developer/resources/ryzen-ai-software.html)
- Watch for Java SDK announcements
- Follow [RyzenAI-SW GitHub](https://github.com/amd/RyzenAI-SW)

**Experimental Workaround:**
- Python microservice with AMD SDK
- Java client via HTTP/gRPC
- Adds complexity and latency
- NOT recommended for production

---

## Migration Path

### Current: All Cloud
```
User → Cloud API → TaskPlanner → ActionExecutor
```

### Target: Hybrid
```
User → TaskPlanner → [Local GPU (primary) | Cloud (fallback)] → ActionExecutor
              ↓
       EmbeddingService (local ONNX)
              ↓
       ForemanMemory (semantic search)
```

---

## Testing Checklist

- [ ] Verify ONNX Runtime works on your system
- [ ] Test embedding generation speed
- [ ] Benchmark semantic search accuracy
- [ ] Verify local LLM works on RTX 4050
- [ ] Test hybrid fallback logic
- [ ] Measure power consumption
- [ ] Compare quality: local vs cloud

---

## Commands

```bash
# Setup
./gradlew downloadModels

# Benchmark embeddings
./gradlew benchmarkEmbeddings

# Test local LLM (optional, requires model)
./gradlew testLocalLLM

# Build
./gradlew build

# Run with local AI
./gradlew runClient
```

---

## Troubleshooting

**Problem:** ONNX Runtime fails to load
**Solution:** Ensure you have the right native libraries for your platform

**Problem:** Out of memory errors
**Solution:** Reduce `gpuLayers` or use CPU inference

**Problem:** Slow embedding generation
**Solution:** Enable CUDA with `onnxruntime_gpu` dependency

**Problem:** Model quality worse than cloud
**Solution:** Fine-tune prompts, try larger models (13B+)

---

## Next Steps

1. **Start small:** Implement local embeddings first
2. **Test thoroughly:** Benchmark on your hardware
3. **Gradual rollout:** A/B test vs. cloud
4. **Monitor:** Track latency, quality, cost
5. **Iterate:** Tune model selection and parameters

---

## Summary

| Aspect | Recommendation |
|--------|----------------|
| **Primary inference** | Local GPU (RTX 4050) |
| **Embeddings** | Local ONNX model |
| **Fallback** | Cloud API (Groq/OpenAI) |
| **NPU** | Not feasible today, monitor AMD |
| **Timeline** | 2-4 weeks for embeddings, 4-8 weeks for LLM |
| **ROI** | 5-10x faster, 90% cost reduction |

---

**Last Updated:** 2026-02-26
**Status:** Ready for Implementation
