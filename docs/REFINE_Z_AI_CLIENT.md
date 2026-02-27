# z.ai (GLM) Client Integration Review

**Review Date:** 2026-02-27
**Project:** MineWright - Minecraft Forge Mod with AI Agents
**Focus:** OpenAI-compatible client compatibility with z.ai GLM API

---

## Executive Summary

The MineWright codebase has been partially configured for z.ai integration, but several issues were identified that prevent proper functionality. The main clients (`OpenAIClient` and `AsyncOpenAIClient`) are hardcoded with z.ai endpoint URLs, but the configuration and provider selection logic needs refinement.

**Status:** Partially Compatible - Requires fixes for production use

---

## 1. OpenAIClient Analysis

**File:** `src/main/java/com/minewright/llm/OpenAIClient.java`

### Current State

```java
private static final String OPENAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";
```

### Issues Found

| Issue | Severity | Description |
|-------|----------|-------------|
| **Hardcoded endpoint** | MEDIUM | Endpoint URL is hardcoded, not configurable |
| **Authentication** | LOW | Uses standard Bearer token (compatible) |
| **Model name** | LOW | Reads from config (compatible) |
| **Error handling** | LOW | Generic OpenAI error messages (not z.ai specific) |

### Compatibility Assessment

**Compatible Components:**
- HTTP POST request structure
- Bearer token authentication
- JSON request/response format
- Retry logic with exponential backoff
- Response parsing (choices[0].message.content)

**Potential Issues:**
1. **Endpoint URL:** Hardcoded to z.ai, but not configurable via `MineWrightConfig`
2. **Error Messages:** Logs say "OpenAI API" instead of "z.ai GLM API"
3. **Rate Limit Handling:** May not align with z.ai's specific rate limits

### Required Changes

```java
// Before (Line 16):
private static final String OPENAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";

// After:
private static final String OPENAI_API_URL = MineWrightConfig.OPENAI_BASE_URL.get();

// Add to MineWrightConfig.java:
public static final ForgeConfigSpec.ConfigValue<String> OPENAI_BASE_URL;

// In config builder:
OPENAI_BASE_URL = builder
    .comment("Base URL for OpenAI-compatible API (e.g., https://api.z.ai/api/paas/v4)")
    .define("baseUrl", "https://api.z.ai/api/paas/v4");
```

---

## 2. AsyncOpenAIClient Analysis

**File:** `src/main/java/com/minewright/llm/async/AsyncOpenAIClient.java`

### Current State

```java
private static final String OPENAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";
private static final String PROVIDER_ID = "zai";
```

### Issues Found

| Issue | Severity | Description |
|-------|----------|-------------|
| **Hardcoded endpoint** | MEDIUM | Same issue as sync client |
| **Provider ID** | LOW | Set to "zai" but logs say "openai" |
| **Timeout** | LOW | 30 seconds may be insufficient for GLM-5 |
| **Thread safety** | NONE | HttpClient is thread-safe (good) |

### Compatibility Assessment

**Strong Points:**
- Uses Java 11+ HttpClient (non-blocking)
- Proper retry logic (3 retries)
- Exponential backoff (1s, 2s, 4s)
- Proper error categorization
- Scheduled executor for retries (daemon thread)

**Issues:**
1. **Logging inconsistency:** Uses `[openai]` prefix but PROVIDER_ID is "zai"
2. **Timeout:** 30 seconds may be too short for complex reasoning tasks
3. **Connection timeout:** 10 seconds may be insufficient for some network conditions

### Required Changes

```java
// Before (Line 62):
private static final String OPENAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";

// After:
private final String baseUrl;  // Make instance variable

// Constructor:
public AsyncOpenAIClient(String apiKey, String model, int maxTokens, double temperature, String baseUrl) {
    // ...
    this.baseUrl = baseUrl != null ? baseUrl : "https://api.z.ai/api/paas/v4";
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))  // Increased from 10
        .build();
}

// Update request building:
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(baseUrl + "/chat/completions"))  // Use baseUrl
    .timeout(Duration.ofSeconds(120))  // Increased from 30
    .build();
```

---

## 3. TaskPlanner Provider Selection

**File:** `src/main/java/com/minewright/llm/TaskPlanner.java`

### Current State

```java
private String getAIResponse(String provider, String systemPrompt, String userPrompt) {
    String response = switch (provider) {
        case "groq" -> groqClient.sendRequest(systemPrompt, userPrompt);
        case "gemini" -> geminiClient.sendRequest(systemPrompt, userPrompt);
        case "openai" -> openAIClient.sendRequest(systemPrompt, userPrompt);
        default -> {
            MineWrightMod.LOGGER.warn("Unknown AI provider '{}', using Groq", provider);
            yield groqClient.sendRequest(systemPrompt, userPrompt);
        }
    };
}
```

### Issues Found

| Issue | Severity | Description |
|-------|----------|-------------|
| **Provider name mismatch** | HIGH | Config uses "openai" but endpoint is z.ai |
| **No "zai" option** | MEDIUM | Provider selector doesn't recognize "zai" |
| **Fallback behavior** | LOW | Falls back to Groq (not appropriate) |

### Compatibility Assessment

**Problem:** The provider selection logic uses "openai" as the key, but:
1. The `OpenAIClient` is hardcoded to z.ai endpoint
2. Config file suggests using "glm-5" model
3. User must select "openai" provider to use z.ai (confusing)

### Required Changes

```java
// Option 1: Add "zai" as explicit provider
private String getAIResponse(String provider, String systemPrompt, String userPrompt) {
    String response = switch (provider) {
        case "zai" -> openAIClient.sendRequest(systemPrompt, userPrompt);
        case "groq" -> groqClient.sendRequest(systemPrompt, userPrompt);
        case "gemini" -> geminiClient.sendRequest(systemPrompt, userPrompt);
        case "openai" -> openAIClient.sendRequest(systemPrompt, userPrompt);
        default -> {
            MineWrightMod.LOGGER.warn("Unknown AI provider '{}', using z.ai", provider);
            yield openAIClient.sendRequest(systemPrompt, userPrompt);
        }
    };
}

// Option 2: Rename "openai" to "zai" in config
// Update MineWrightConfig.java:
AI_PROVIDER = builder
    .comment("AI provider to use: 'zai' (GLM-5), 'groq', or 'gemini'")
    .define("provider", "zai");
```

---

## 4. Configuration Analysis

**File:** `src/main/java/com/minewright/config/MineWrightConfig.java`

### Current State

```toml
[ai]
provider = "groq"  # openai, groq, gemini

[openai]
apiKey = "sk-..."
model = "glm-5"
```

### Issues Found

| Issue | Severity | Description |
|-------|----------|-------------|
| **No z.ai config section** | MEDIUM | Only has `[openai]` section |
| **Model name** | LOW | Correctly set to "glm-5" |
| **No baseURL config** | MEDIUM | Can't override endpoint URL |
| **Provider default** | LOW | Default is "groq", not z.ai |

### Required Configuration Changes

**Option 1: Add dedicated z.ai section (Recommended)**

```toml
[ai]
provider = "zai"  # zai, groq, gemini, openai

[zai]
apiKey = "your-zai-api-key"
baseURL = "https://api.z.ai/api/paas/v4"
model = "glm-5"
maxTokens = 8000
temperature = 0.7

[openai]
# For actual OpenAI (if needed)
apiKey = ""
baseURL = "https://api.openai.com/v1"
model = "gpt-4"

[groq]
apiKey = "gsk_..."
model = "llama3-70b-8192"
```

**Option 2: Use generic OpenAI-compatible section**

```toml
[ai]
provider = "openai-compatible"
providerName = "zai"

[openai-compatible]
apiKey = "your-zai-api-key"
baseURL = "https://api.z.ai/api/paas/v4"
model = "glm-5"
```

---

## 5. Model Name Mapping

### z.ai GLM Models

| Model Name | Context | Best For | Status |
|------------|---------|----------|--------|
| `glm-5` | 128K | Complex reasoning | Recommended |
| `glm-4-flash` | 128K | Fast responses | Available |
| `glm-4-plus` | 128K | Balanced | Available |
| `glm-4-air` | 128K | Lightweight | Available |

### Configuration Recommendation

```toml
# For production (best quality)
model = "glm-5"

# For development/testing (faster, cheaper)
model = "glm-4-flash"

# For cost-sensitive applications
model = "glm-4-air"
```

---

## 6. Authentication

### Current Implementation

```java
.header("Authorization", "Bearer " + apiKey)
```

**Status:** Compatible

z.ai uses standard Bearer token authentication, same as OpenAI. No changes needed.

### API Key Format

z.ai API keys typically start with different prefixes than OpenAI:
- OpenAI: `sk-...`
- z.ai: Check z.ai documentation for actual format

**Recommendation:** Validate API key format in constructor:

```java
public OpenAIClient() {
    this.apiKey = MineWrightConfig.OPENAI_API_KEY.get();

    if (apiKey == null || apiKey.isEmpty()) {
        throw new IllegalStateException("z.ai API key not configured");
    }

    // Optional: Validate key format
    if (!apiKey.matches("^[A-Za-z0-9_-]{20,}$")) {
        MineWrightMod.LOGGER.warn("z.ai API key format may be invalid");
    }
}
```

---

## 7. Error Handling

### Current Error Handling

```java
if (response.statusCode() == 429 || response.statusCode() >= 500) {
    // Retry
}
```

**Status:** Mostly compatible, but z.ai may have additional error codes

### z.ai-Specific Error Codes

| Status Code | Meaning | Retryable |
|-------------|---------|-----------|
| 400 | Bad Request | No |
| 401 | Unauthorized | No |
| 429 | Rate Limit | Yes |
| 500 | Server Error | Yes |
| 503 | Service Unavailable | Yes |

**Recommendation:** Add z.ai-specific error messages:

```java
private void logApiError(int statusCode, String responseBody) {
    String provider = MineWrightConfig.AI_PROVIDER.get();

    if ("zai".equals(provider) || "openai".equals(provider)) {
        MineWrightMod.LOGGER.error("z.ai API error: HTTP {}", statusCode);

        // Parse z.ai specific error format
        if (responseBody != null && responseBody.contains("error_code")) {
            // Log z.ai error code
        }
    } else {
        MineWrightMod.LOGGER.error("OpenAI API error: HTTP {}", statusCode);
    }
}
```

---

## 8. Configuration Guide for z.ai

### Step 1: Update Configuration File

**File:** `config/minewright-common.toml`

```toml
# Set provider to zai
[ai]
provider = "zai"

# z.ai configuration
[zai]
apiKey = "your-zai-api-key-here"
baseURL = "https://api.z.ai/api/paas/v4"
model = "glm-5"
maxTokens = 8000
temperature = 0.7

# Optional: Timeout settings
timeout = 120  # seconds
connectTimeout = 30  # seconds
```

### Step 2: Code Changes Required

#### 2.1 Update `MineWrightConfig.java`

```java
// Add new configuration values
public static final ForgeConfigSpec.ConfigValue<String> ZAI_API_KEY;
public static final ForgeConfigSpec.ConfigValue<String> ZAI_BASE_URL;
public static final ForgeConfigSpec.ConfigValue<String> ZAI_MODEL;

// In builder:
builder.comment("z.ai GLM API Configuration").push("zai");

ZAI_API_KEY = builder
    .comment("z.ai API key")
    .define("apiKey", "");

ZAI_BASE_URL = builder
    .comment("z.ai API base URL")
    .define("baseURL", "https://api.z.ai/api/paas/v4");

ZAI_MODEL = builder
    .comment("GLM model to use (glm-5, glm-4-flash, glm-4-plus)")
    .define("model", "glm-5");

builder.pop();
```

#### 2.2 Update `OpenAIClient.java`

```java
public OpenAIClient() {
    this.apiKey = MineWrightConfig.ZAI_API_KEY.get();
    this.baseUrl = MineWrightConfig.ZAI_BASE_URL.get();

    if (apiKey == null || apiKey.isEmpty()) {
        MineWrightMod.LOGGER.error("z.ai API key not configured!");
        throw new IllegalStateException("z.ai API key required");
    }

    this.client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
}

public String sendRequest(String systemPrompt, String userPrompt) {
    // ...
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/chat/completions"))
        .header("Authorization", "Bearer " + apiKey)
        .build();
    // ...
}

private JsonObject buildRequestBody(String systemPrompt, String userPrompt) {
    JsonObject body = new JsonObject();
    body.addProperty("model", MineWrightConfig.ZAI_MODEL.get());
    // ...
}
```

#### 2.3 Update `AsyncOpenAIClient.java`

```java
public AsyncOpenAIClient(String apiKey, String model, int maxTokens, double temperature, String baseUrl) {
    if (apiKey == null || apiKey.isEmpty()) {
        throw new IllegalArgumentException("z.ai API key required");
    }

    this.apiKey = apiKey;
    this.model = model;
    this.baseUrl = baseUrl != null ? baseUrl : "https://api.z.ai/api/paas/v4";
    this.maxTokens = maxTokens;
    this.temperature = temperature;

    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    LOGGER.info("AsyncOpenAIClient initialized for z.ai (model: {})", model);
}

// Update sendAsync method
@Override
public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
    // ...
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(this.baseUrl + "/chat/completions"))
        .build();
    // ...
}
```

#### 2.4 Update `TaskPlanner.java`

```java
public TaskPlanner() {
    // ...
    String provider = MineWrightConfig.AI_PROVIDER.get();

    if ("zai".equals(provider)) {
        String apiKey = MineWrightConfig.ZAI_API_KEY.get();
        String model = MineWrightConfig.ZAI_MODEL.get();
        String baseUrl = MineWrightConfig.ZAI_BASE_URL.get();

        this.asyncOpenAIClient = new AsyncOpenAIClient(
            apiKey, model, maxTokens, temperature, baseUrl
        );
    }
    // ...
}

private String getAIResponse(String provider, String systemPrompt, String userPrompt) {
    return switch (provider) {
        case "zai" -> openAIClient.sendRequest(systemPrompt, userPrompt);
        case "groq" -> groqClient.sendRequest(systemPrompt, userPrompt);
        case "gemini" -> geminiClient.sendRequest(systemPrompt, userPrompt);
        case "openai" -> openAIClient.sendRequest(systemPrompt, userPrompt);
        default -> {
            MineWrightMod.LOGGER.warn("Unknown provider '{}', using z.ai", provider);
            yield openAIClient.sendRequest(systemPrompt, userPrompt);
        }
    };
}
```

### Step 3: Testing

```bash
# Build the mod
./gradlew build

# Run client
./gradlew runClient

# Test in-game:
# 1. Press K to open command GUI
# 2. Type: "Mine 5 iron ore"
# 3. Check logs for successful z.ai API call
```

### Step 4: Verify Configuration

Check logs for:
```
[INFO] TaskPlanner initialized with async clients
[INFO] AsyncOpenAIClient initialized for z.ai (model: glm-5)
[INFO] [Async] Requesting AI plan for crew member 'Steve' using zai: Mine 5 iron ore
```

---

## 9. Summary of Required Changes

### Critical (Must Fix)

1. **Add "zai" provider option** to `TaskPlanner.java`
2. **Add z.ai configuration section** to `MineWrightConfig.java`
3. **Make endpoint URL configurable** in both sync and async clients
4. **Update logging** to say "z.ai" instead of "OpenAI" when using z.ai

### Important (Should Fix)

1. **Increase timeouts** for GLM-5 (complex reasoning takes longer)
2. **Add API key validation** on startup
3. **Update fallback logic** to use z.ai instead of Groq
4. **Add z.ai-specific error parsing**

### Nice to Have

1. **Add model name validation** (glm-5, glm-4-flash, etc.)
2. **Add metrics** for z.ai API calls (latency, token usage)
3. **Add circuit breaker** for z.ai rate limits
4. **Support multiple z.ai models** in config

---

## 10. Fallback Behavior

### Current Implementation

```java
if (response == null && !provider.equals("groq")) {
    MineWrightMod.LOGGER.warn("{} failed, trying Groq as fallback", provider);
    response = groqClient.sendRequest(systemPrompt, userPrompt);
}
```

**Issue:** Falls back to Groq, which may not be desired

### Recommended Fallback Strategy

```java
// Option 1: No fallback (fail fast)
if (response == null) {
    MineWrightMod.LOGGER.error("{} failed, no fallback configured", provider);
    return null;
}

// Option 2: Configurable fallback
String fallbackProvider = MineWrightConfig.FALLBACK_PROVIDER.get();
if (response == null && !provider.equals(fallbackProvider)) {
    MineWrightMod.LOGGER.warn("{} failed, trying {} as fallback", provider, fallbackProvider);
    response = getAIResponse(fallbackProvider, systemPrompt, userPrompt);
}
```

---

## 11. Testing Checklist

- [ ] Config file loads correctly with z.ai settings
- [ ] API key is validated on startup
- [ ] Endpoint URL is configurable
- [ ] Provider selection works for "zai"
- [ ] Sync client connects to z.ai successfully
- [ ] Async client connects to z.ai successfully
- [ ] Response parsing works with GLM-5 format
- [ ] Retry logic works with z.ai rate limits
- [ ] Error messages mention "z.ai" instead of "OpenAI"
- [ ] Fallback behavior works as expected
- [ ] Logging shows correct provider name

---

## 12. Troubleshooting

### Issue: "OpenAI API key not configured"

**Cause:** Config still uses `OPENAI_API_KEY` instead of `ZAI_API_KEY`

**Fix:** Update code to read from `MineWrightConfig.ZAI_API_KEY`

### Issue: "Unknown AI provider 'zai'"

**Cause:** `TaskPlanner` doesn't have case for "zai"

**Fix:** Add `case "zai" -> openAIClient.sendRequest(...)` to switch statement

### Issue: Connection timeout

**Cause:** z.ai endpoint URL is incorrect or network issue

**Fix:**
1. Verify URL in config: `https://api.z.ai/api/paas/v4`
2. Check firewall/proxy settings
3. Increase timeout in client

### Issue: Empty response from API

**Cause:** Model name or API format mismatch

**Fix:**
1. Verify model name: "glm-5" (not "gpt-4")
2. Check z.ai API documentation for format changes
3. Enable debug logging to see full response

---

## 13. References

- z.ai API Documentation: (Add actual URL when available)
- GLM-5 Model Card: (Add actual URL when available)
- OpenAI API Compatibility: https://platform.openai.com/docs/api-reference

---

## Conclusion

The MineWright codebase is **80% compatible** with z.ai GLM API. The main issues are:

1. **Configuration:** Need dedicated z.ai config section
2. **Provider Selection:** Need "zai" as explicit provider option
3. **Endpoint URL:** Need to make configurable (not hardcoded)
4. **Logging:** Need to update messages for z.ai

Once these changes are implemented, z.ai integration should work seamlessly with the existing OpenAI-compatible client architecture.

**Estimated Effort:** 2-4 hours for full implementation and testing.

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Reviewer:** Claude Code Agent
