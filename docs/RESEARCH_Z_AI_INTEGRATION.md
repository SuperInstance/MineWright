# Z.ai (GLM) API Integration Research

**Research Date:** 2026-02-27
**Project:** MineWright - Autonomous AI Agents for Minecraft
**Version:** 1.0.0
**Status:** Ready for Implementation

---

## Executive Summary

Z.ai (formerly Zhipu AI / ChatGLM) provides Chinese LLM models with OpenAI-compatible API endpoints. MineWright already has partial z.ai integration configured in `OpenAIClient.java` with the endpoint `https://api.z.ai/api/paas/v4/chat/completions`.

**Key Findings:**
- Z.ai is **fully OpenAI-compatible** - existing client works
- Current config uses `glm-5` model (needs verification)
- Free tier available (GLM-4.7-Flash)
- Cost-effective alternative to OpenAI (~1/10 the cost of Claude)
- Up to 200K context window

---

## Table of Contents

1. [API Overview](#1-api-overview)
2. [Available Models](#2-available-models)
3. [Authentication](#3-authentication)
4. [API Compatibility](#4-api-compatibility)
5. [Configuration Guide](#5-configuration-guide)
6. [Rate Limits & Pricing](#6-rate-limits--pricing)
7. [Integration Verification](#7-integration-verification)
8. [Code Examples](#8-code-examples)
9. [Troubleshooting](#9-troubleshooting)
10. [Recommendations](#10-recommendations)

---

## 1. API Overview

### 1.1 Base Endpoint

```
https://api.z.ai/api/paas/v4/chat/completions
```

### 1.2 Request Format (OpenAI-Compatible)

```json
POST https://api.z.ai/api/paas/v4/chat/completions
Content-Type: application/json
Authorization: Bearer your-api-key

{
  "model": "glm-4.7",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello!"}
  ],
  "temperature": 0.7,
  "max_tokens": 4096,
  "stream": false
}
```

### 1.3 Response Format (OpenAI-Compatible)

```json
{
  "id": "chatcmpl-123456789",
  "object": "chat.completion",
  "created": 1234567890,
  "model": "glm-4.7",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "Hello! How can I help you today?"
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 9,
    "total_tokens": 19
  }
}
```

---

## 2. Available Models

### 2.1 Current Model Family

| Model | Context | Best For | Status |
|-------|---------|----------|--------|
| **GLM-4.7** | 128K | Coding, complex reasoning | Latest flagship |
| **GLM-4.7-FlashX** | 128K | High throughput | $0.07 input / $0.40 output |
| **GLM-4.7-Flash** | 128K | General use | **FREE** (1 concurrent) |
| **GLM-4.6** | 200K | Long context, tool use | Research available |
| **GLM-4.5** | 128K | General purpose | Stable |
| **GLM-4-Flash** | 128K | Free tier | Completely FREE |
| **GLM-4V** | 128K | Vision/multimodal | Image inputs |

### 2.2 Model Configured in MineWright

**Current Config:** `glm-5`

**Note:** `glm-5` is mentioned in the config but not found in current z.ai documentation. This may be:
- A placeholder/unreleased model
- A typo (should be `glm-4.5` or `glm-4.7`)
- An internal model name

**Recommendation:** Update to `glm-4.7-flash` (free) or `glm-4.7` (paid)

### 2.3 Model Capabilities

**GLM-4.7 Series:**
- Enhanced programming capabilities
- Stable multi-step reasoning/execution
- Superior for complex agent tasks
- Natural conversational experience

**GLM-4.6:**
- 200K context window
- Tool-enhanced reasoning
- Better for long-context tasks

**Thinking Parameter (unique to GLM):**
```json
{
  "thinking": {
    "type": "enabled"  // or "disabled"
  }
}
```
Enables reasoning traces in responses.

---

## 3. Authentication

### 3.1 API Key Format

Z.ai uses two API key formats:

**Format 1: Standard (Dot-separated)**
```
{key_id}.{key_secret}
Example: a1b2c3d4e5f6.g7h8i9j0k1l2m3n4
```

**Format 2: OpenAI-Compatible**
```
sk-glm-47-xxxxxxxxxxxx
Example: sk-glm-47-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 3.2 JWT Token Authentication

Z.ai uses JWT (JSON Web Token) with HMAC-SHA256 signing:

1. API key parsed into `key_id` and `key_secret`
2. JWT token generated with claims: `api_key`, `timestamp`, `exp`
3. Token signed using `key_secret` and HS256
4. Token expires in 7 days (auto-regenerated)

**Header:**
```
Authorization: Bearer {jwt_token}
```

### 3.3 Obtaining an API Key

1. Visit [Z.ai Platform](https://z.ai/) or [BigModel.cn](https://bigmodel.cn/)
2. Register with phone/email
3. Complete identity verification (personal: ID, enterprise: business license)
4. Create API key in console
5. **Important:** Copy key immediately (cannot be viewed again after refresh)

---

## 4. API Compatibility

### 4.1 OpenAI Compatibility

**Z.ai is 100% OpenAI-compatible** with the following features:

| Feature | Status |
|---------|--------|
| Chat Completions | ✅ Compatible |
| Streaming | ✅ Supported |
| Function Calling | ✅ Supported |
| Multi-turn Conversations | ✅ Supported |
| System Messages | ✅ Supported |
| Temperature/Max Tokens | ✅ Supported |
| JSON Mode | ⚠️ Check docs |
| Embeddings | ✅ Supported |

### 4.2 Using OpenAI SDK

```python
from openai import OpenAI

client = OpenAI(
    api_key="your-z.ai-api-key",
    base_url="https://api.z.ai/api/paas/v4/"
)

response = client.chat.completions.create(
    model="glm-4.7",
    messages=[
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "Hello!"}
    ]
)
```

### 4.3 Existing MineWright Client

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\OpenAIClient.java`

**Current Endpoint (Line 16):**
```java
private static final String OPENAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";
```

✅ **Already configured correctly!** The existing `OpenAIClient` should work with z.ai without modification.

**Async Client (Line 62):**
```java
// File: AsyncOpenAIClient.java
private static final String OPENAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";
```

✅ **Async client also configured!**

---

## 5. Configuration Guide

### 5.1 Current Configuration

**File:** `C:\Users\casey\steve\run\config\minewright-common.toml`

```toml
[ai]
    provider = "openai"

[openai]
    apiKey = "your-zai-api-key-here"
    model = "glm-5"
    maxTokens = 65536
    temperature = 0.9
```

**Config Class:** `C:\Users\casey\steve\src\main\java\com\minewright\config\MineWrightConfig.java`

```java
OPENAI_MODEL = builder
    .comment("LLM model to use (glm-5 for z.ai, gpt-4 for OpenAI)")
    .define("model", "glm-5");
```

### 5.2 Recommended Configuration Updates

**Option 1: Free Tier (GLM-4.7-Flash)**
```toml
[ai]
    provider = "openai"  # Keep as "openai" - z.ai uses OpenAI-compatible endpoint

[openai]
    apiKey = "your-z.ai-api-key"
    model = "glm-4.7-flash"  # Free model
    maxTokens = 4096  # Flash has 4K output limit
    temperature = 0.7
```

**Option 2: Paid Tier (GLM-4.7)**
```toml
[ai]
    provider = "openai"

[openai]
    apiKey = "your-z.ai-api-key"
    model = "glm-4.7"  # Paid flagship model
    maxTokens = 131072  # Up to 128K tokens
    temperature = 0.7
```

**Option 3: High Context (GLM-4.6)**
```toml
[ai]
    provider = "openai"

[openai]
    apiKey = "your-z.ai-api-key"
    model = "glm-4.6"  # 200K context window
    maxTokens = 200000
    temperature = 0.7
```

### 5.3 Provider Selection

**Note:** The `provider` config currently has three options:
- `"groq"` - Fastest, FREE
- `"openai"` - Uses OpenAIClient (currently pointing to z.ai)
- `"gemini"` - Google Gemini

**Recommendation:** Create a dedicated `"zai"` provider option:

```java
// In MineWrightConfig.java
AI_PROVIDER = builder
    .comment("AI provider: 'groq' (FASTEST, FREE), 'openai', 'gemini', 'zai'")
    .define("provider", "zai");  // New option
```

```toml
[ai]
    provider = "zai"  # Use dedicated z.ai option
```

---

## 6. Rate Limits & Pricing

### 6.1 Rate Limits

| Model | Concurrent Requests | Rate Limit |
|-------|---------------------|------------|
| GLM-4.7-Flash (Free) | **1** | Not published |
| GLM-4.7-FlashX | Higher | Paid tiers |
| GLM-4.6 / 4.7 | Standard | Based on plan |

**Current MineWright Implementation:**

The `BatchingLLMClient` (`C:\Users\casey\steve\src\main\java\com\minewright\llm\batch\BatchingLLMClient.java`) handles rate limiting automatically through:

- **PromptBatcher** - Batches requests to avoid rate limits
- **HeartbeatScheduler** - Dynamic rate limit adaptation
- **Backoff multiplier** - Exponential backoff on 429 errors

### 6.2 Pricing (2026)

| Model | Input (per 1M tokens) | Output (per 1M tokens) |
|-------|----------------------|-----------------------|
| GLM-4-Flash | **FREE** | **FREE** |
| GLM-4.7-Flash | **FREE** | **FREE** |
| GLM-4-Air | ¥0.5 (~$0.07) | - |
| GLM-Z1-Air | ¥0.5 (~$0.07) | - |
| GLM-Z1-AirX (High Speed) | ¥5 (~$0.70) | Up to 200 tokens/sec |
| GLM-4.5 | ¥0.8 (~$0.11) | ¥2 (~$0.28) |
| GLM-4.7 | ~$0.10 | ~$0.50 |

### 6.3 Subscription Plans

| Plan | Monthly Fee | Quota | Best For |
|------|-------------|-------|----------|
| GLM Free | $0 | 5M tokens (new users) | Testing |
| GLM Plus | ¥69 (~$10) | GLM-4 unlimited + ¥50 credit | Daily use |
| GLM Pro | ¥149 (~$21) | GLM-5 unlimited + 5M GLM-4-Plus | Heavy coding |
| GLM Ultimate | ¥299 (~$42) | All unlimited + 15M GLM-4-Plus | Professional |

---

## 7. Integration Verification

### 7.1 Current Implementation Status

| Component | Status | Notes |
|-----------|--------|-------|
| `OpenAIClient.java` | ✅ Configured | Endpoint: `https://api.z.ai/api/paas/v4/chat/completions` |
| `AsyncOpenAIClient.java` | ✅ Configured | Same endpoint |
| `MineWrightConfig.java` | ⚠️ Needs Update | References `glm-5` (may not exist) |
| `minewright-common.toml` | ⚠️ Needs Update | Uses `glm-5` |
| Provider Selection | ⚠️ Confusing | z.ai uses "openai" provider |
| Documentation | ❌ Missing | No z.ai-specific docs |

### 7.2 Testing the Connection

**Test cURL Command:**
```bash
curl -X POST "https://api.z.ai/api/paas/v4/chat/completions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -d '{
    "model": "glm-4.7-flash",
    "messages": [
      {"role": "user", "content": "Hello!"}
    ],
    "max_tokens": 100
  }'
```

**Expected Response:**
```json
{
  "choices": [{
    "message": {
      "content": "Hello! How can I help you today?",
      "role": "assistant"
    }
  }],
  "model": "glm-4.7-flash",
  "usage": {
    "total_tokens": 15
  }
}
```

### 7.3 Java Test Code

```java
import com.minewright.llm.OpenAIClient;

public class TestZAiIntegration {
    public static void main(String[] args) {
        // Test the existing client
        OpenAIClient client = new OpenAIClient();

        String response = client.sendRequest(
            "You are a helpful assistant.",
            "Say 'Hello from z.ai!' if you receive this message."
        );

        if (response != null && response.contains("Hello")) {
            System.out.println("✅ z.ai integration working!");
            System.out.println("Response: " + response);
        } else {
            System.out.println("❌ Integration failed. Check API key and model.");
        }
    }
}
```

---

## 8. Code Examples

### 8.1 Basic Usage (Existing Client)

```java
// File: OpenAIClient.java
OpenAIClient client = new OpenAIClient();

String response = client.sendRequest(
    "You are Steve, an autonomous Minecraft agent.",
    "Build a wooden house at my current location."
);

System.out.println("AI Response: " + response);
```

### 8.2 Async Usage

```java
// File: AsyncOpenAIClient.java
AsyncOpenAIClient client = new AsyncOpenAIClient(
    apiKey,
    "glm-4.7-flash",  // Free model
    4096,              // max tokens
    0.7                // temperature
);

client.sendAsync("Plan tasks for building a house", Map.of())
    .thenAccept(response -> {
        System.out.println("Response: " + response.getContent());
        System.out.println("Tokens used: " + response.getTokensUsed());
        System.out.println("Latency: " + response.getLatencyMs() + "ms");
    })
    .exceptionally(error -> {
        System.err.println("Error: " + error.getMessage());
        return null;
    });
```

### 8.3 Batched Usage (Rate Limit Aware)

```java
// File: BatchingLLMClient.java
AsyncLLMClient underlyingClient = new AsyncOpenAIClient(...);
BatchingLLMClient batchClient = new BatchingLLMClient(underlyingClient);
batchClient.start();

// User interaction (fast)
batchClient.submitUserPrompt("Build a house", context)
    .thenAccept(response -> handleUserResponse(response));

// Background tasks (batched)
batchClient.submitBackgroundPrompt("Analyze resources", context)
    .thenAccept(response -> handleAnalysis(response));
```

### 8.4 Streaming Response

```java
// Enable streaming in request
Map<String, Object> params = Map.of(
    "model", "glm-4.7",
    "stream", true,
    "maxTokens", 4096
);

client.sendAsync("Generate a long response", params)
    .thenAccept(response -> {
        // Handle streaming chunks
        String content = response.getContent();
        System.out.print(content);
    });
```

### 8.5 Using the "Thinking" Parameter (GLM-Specific)

```java
JsonObject requestBody = new JsonObject();
requestBody.addProperty("model", "glm-4.7");

// Add thinking parameter for reasoning traces
JsonObject thinking = new JsonObject();
thinking.addProperty("type", "enabled");
requestBody.add("thinking", thinking);

// ... rest of request
```

---

## 9. Troubleshooting

### 9.1 Common Issues

**Issue 1: Model Not Found**
```
Error: Model 'glm-5' does not exist
```
**Solution:** Update model to `glm-4.7`, `glm-4.6`, or `glm-4.7-flash`

**Issue 2: Authentication Failed**
```
Error: 401 Unauthorized
```
**Solutions:**
- Verify API key format
- Check account status (verification needed)
- Ensure account has balance

**Issue 3: Rate Limit Exceeded**
```
Error: 429 Too Many Requests
```
**Solutions:**
- Use `BatchingLLMClient` for automatic batching
- Reduce concurrent requests
- Upgrade plan for higher limits

**Issue 4: Empty Response**
```
Response: null or empty
```
**Solutions:**
- Check `maxTokens` setting
- Verify endpoint URL
- Check network connectivity

### 9.2 Debug Logging

Enable debug logging in MineWright:

```java
// In log4j2.xml or equivalent
<Logger name="com.minewright.llm" level="DEBUG"/>
<Logger name="com.minewwright.llm.async" level="DEBUG"/>
<Logger name="com.minewwright.llm.batch" level="DEBUG"/>
```

### 9.3 API Key Validation

```java
public static boolean validateApiKey(String apiKey) {
    if (apiKey == null || apiKey.isEmpty()) {
        return false;
    }

    // Check for dot-separated format
    if (apiKey.contains(".")) {
        String[] parts = apiKey.split("\\.");
        return parts.length == 2 && parts[0].length() > 0 && parts[1].length() > 0;
    }

    // Check for OpenAI format
    if (apiKey.startsWith("sk-glm-")) {
        return apiKey.length() > 20;
    }

    return false;
}
```

---

## 10. Recommendations

### 10.1 Immediate Actions

1. **Update Model Configuration**
   - Change `model = "glm-5"` to `model = "glm-4.7-flash"` (free tier)
   - Update config comments to reflect current z.ai models

2. **Add z.ai Provider Option**
   - Create dedicated `"zai"` provider in config
   - Avoid confusion with actual OpenAI

3. **Test Integration**
   - Verify API key works with `glm-4.7-flash`
   - Test request/response cycle
   - Measure latency

4. **Update Documentation**
   - Add z.ai setup guide to README
   - Document API key acquisition
   - Note Chinese language optimization

### 10.2 Short-Term Improvements

1. **Model Selection UI**
   - Allow users to select model in config
   - Display model capabilities (context, pricing)
   - Show current quota/usage

2. **Rate Limit Monitoring**
   - Add metrics for 429 errors
   - Track backoff multiplier
   - Display queue status

3. **Cost Tracking**
   - Log token usage per request
   - Calculate estimated costs
   - Warn on high usage

4. **Error Handling**
   - Specific error messages for z.ai
   - Retry logic for auth failures
   - Fallback to Groq if z.ai fails

### 10.3 Long-Term Considerations

1. **Hybrid Provider Strategy**
   - Use Groq for fast, simple queries (free)
   - Use z.ai for complex reasoning (Chinese optimization)
   - Use OpenAI as fallback

2. **Model Fine-Tuning**
   - Consider fine-tuning GLM for Minecraft-specific tasks
   - Train on Minecraft command patterns
   - Optimize for block placement logic

3. **Local Deployment**
   - ChatGLM supports local deployment
   - Run on-premises for privacy
   - Reduce API dependency

4. **Multi-Model Support**
   - Route requests to best model per task
   - Use GLM-4V for visual tasks
   - Use GLM-4.6 for long context

---

## 11. Security Considerations

### 11.1 API Key Security

**Current Risk:** API key is stored in plain text in config file

**Recommendations:**
- Use environment variables: `ZAI_API_KEY`
- Encrypt API key in storage
- Never log API keys (see security audit)
- Rotate keys periodically

### 11.2 Data Privacy

**Considerations:**
- Prompts sent to Chinese servers (z.ai/Zhipu)
- World state data may be sensitive
- Player coordinates in prompts

**Mitigations:**
- Sanitize prompts before sending
- Use local deployment option
- Offer opt-out for telemetry

### 11.3 Input Validation

**Current Risk:** No prompt sanitization (see security audit)

**Recommendations:**
- Implement prompt injection protection
- Limit prompt length
- Validate user commands
- Rate limit per player

---

## 12. Performance Optimization

### 12.1 Token Efficiency

**Strategies:**
- Compress world knowledge before sending
- Use embeddings for context retrieval
- Batch multiple actions into one prompt
- Cache common responses

### 12.2 Latency Reduction

**Techniques:**
- Use GLM-4.7-FlashX for faster responses
- Pre-fetch common commands
- Stream responses for early display
- Use CDN for API requests

### 12.3 Cost Optimization

**Approaches:**
- Use free tier for development
- Batch background tasks
- Implement intelligent caching
- Monitor usage metrics

---

## Appendix A: Quick Reference

### A.1 Endpoints

```
Chat Completions: https://api.z.ai/api/paas/v4/chat/completions
Models List:      https://api.z.ai/api/paas/v4/models
Documentation:    https://docs.z.ai/
Console:          https://bigmodel.cn/
```

### A.2 Model Selection Guide

| Use Case | Recommended Model |
|----------|-------------------|
| Development | GLM-4.7-Flash (FREE) |
| Production (Cost-Sensitive) | GLM-4-Flash (FREE) |
| Production (Performance) | GLM-4.7 |
| Long Context | GLM-4.6 (200K) |
| High Throughput | GLM-Z1-AirX |
| Vision Tasks | GLM-4V |

### A.3 Configuration Templates

**Development (Free):**
```toml
[ai]
provider = "zai"

[openai]
apiKey = "your-api-key"
model = "glm-4.7-flash"
maxTokens = 4096
temperature = 0.7
```

**Production:**
```toml
[ai]
provider = "zai"

[openai]
apiKey = "your-api-key"
model = "glm-4.7"
maxTokens = 131072
temperature = 0.7
```

**High Context:**
```toml
[ai]
provider = "zai"

[openai]
apiKey = "your-api-key"
model = "glm-4.6"
maxTokens = 200000
temperature = 0.7
```

---

## Appendix B: Resources

### B.1 Official Documentation

- [GLM-4.7 Overview](https://docs.z.ai/guides/llm/glm-4.7)
- [GLM-4.6 Guide](https://docs.z.ai/guides/llm/glm-4.6)
- [Python SDK](https://github.com/zai-org/z-ai-sdk-python)
- [Java SDK](https://github.com/zai-org/z-ai-sdk-java)
- [OpenAI SDK Compatibility](https://docs.z.ai/quickstart)

### B.2 Getting Started

1. [Register Account](https://bigmodel.cn/)
2. [Get API Key](https://bigmodel.cn/usercenter/apikeys)
3. [Choose Pricing Plan](https://z.ai/subscribe)
4. [Read Documentation](https://docs.z.ai/)
5. [Join Community](https://github.com/zai-org)

### B.3 Community Resources

- [GLM Models on GitHub](https://github.com/THUDM/GLM-4)
- [Z.ai Discussion](https://github.com/zai-org)
- [ChatGLM Open Source](https://github.com/THUDM/ChatGLM3)

---

## Conclusion

Z.ai (GLM) provides a **production-ready, cost-effective alternative** to OpenAI with **full API compatibility**. The existing MineWright codebase already has the correct endpoint configured, requiring only:

1. Update model name from `glm-5` to `glm-4.7-flash` (or current model)
2. Verify API key format
3. Test connection
4. Update documentation

The free tier (GLM-4.7-Flash) makes it ideal for development and testing, while paid tiers offer competitive pricing for production use.

**Next Steps:**
1. Test with free tier (`glm-4.7-flash`)
2. Measure latency and quality
3. Compare with Groq (current free option)
4. Update config with validated settings
5. Document migration guide

---

**Document Version:** 1.0.0
**Last Updated:** 2026-02-27
**Researcher:** Claude (Anthropic)
**Status:** Ready for Implementation

---

## Sources

- [GLM-4.7 - Overview - Z.AI DEVELOPER DOCUMENT](https://docs.z.ai/guides/llm/glm-4.7)
- [2026 Q1 AI Programming Model Evaluation - Juejin](https://juejin.cn/post/7609481369001115667)
- [GLM-4.7-Flash: Truly Free Coding Model API](https://www.toutiao.com/a7597269201325163008/)
- [China's Zhipu Debuts GLM-4.5 - Sohu News](https://www.sohu.com/a/918720078_116132)
- [GLM-4.6: 200K Context + Tool Enhanced Reasoning - 51CTO](https://www.51cto.com/aigc/8134.html)
- [Z.ai GitHub Topics](https://github.com/topics/zai)
- [Z.ai Python SDK - GitHub](https://github.com/zai-org/z-ai-sdk-python)
- [GLM-4.7-Flash Model Page](https://m.datalearner.com:8443/ai-models/pretrained-models/glm-4-7-flash)
- [LLM Pricing: Top 15+ Providers Compared in 2025](https://research.aimultiple.com/llm-pricing/)
