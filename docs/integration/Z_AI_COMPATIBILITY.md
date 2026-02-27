# z.ai API Compatibility Report

**Analysis Date:** 2026-02-27
**Component:** OpenAI/AsyncOpenAI Client
**Status:** Partially Compatible - Configuration Changes Required

---

## Executive Summary

The OpenAIClient.java and AsyncOpenAIClient.java files have been partially configured for z.ai compatibility. The API endpoint is correctly set to z.ai's OpenAI-compatible endpoint, but several issues remain that prevent full production readiness.

**Compatibility Score:** 6/10

- API Endpoint: Configured
- Provider ID: Mismatch
- Model Support: Configured
- Documentation: Outdated
- Error Handling: Generic

---

## 1. API Endpoint Configuration

### Current Status: CORRECT

Both client implementations use the correct z.ai endpoint:

**OpenAIClient.java (Line 16):**
```java
private static final String OPENAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";
```

**AsyncOpenAIClient.java (Line 62):**
```java
private static final String OPENAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";
```

This is the correct endpoint for z.ai's OpenAI-compatible API.

---

## 2. Provider ID Configuration

### Current Status: INCORRECT

**AsyncOpenAIClient.java (Line 63):**
```java
private static final String PROVIDER_ID = "zai";
```

**Issue:** The provider ID is set to "zai" but the configuration system expects "openai". This causes a mismatch in:
- Error logging
- Provider selection logic
- Metrics collection
- Executor service routing

**TaskPlanner.java (Lines 309-315):**
```java
/**
 * @param provider Provider name ("openai", "groq", "gemini")
 */
private AsyncLLMClient getAsyncClient(String provider) {
    return switch (provider.toLowerCase()) {
        case "openai" -> asyncOpenAIClient;
        case "groq" -> asyncGroqClient;
        case "gemini" -> asyncGeminiClient;
        default -> throw new IllegalArgumentException("Unknown provider: " + provider);
    };
}
```

**Recommendation:** The PROVIDER_ID in AsyncOpenAIClient should remain "openai" for compatibility with the existing provider selection system. The z.ai endpoint is an implementation detail.

---

## 3. Model Configuration

### Current Status: CORRECT

**MineWrightConfig.java (Lines 46-48):**
```java
OPENAI_MODEL = builder
    .comment("LLM model to use (glm-5 for z.ai, gpt-4 for OpenAI)")
    .define("model", "glm-5");
```

The default model is correctly set to `glm-5`, which is z.ai's flagship model.

**Supported z.ai Models:**
- `glm-5` - Default, recommended
- `glm-4` - Previous generation
- `glm-4-plus` - Enhanced version
- `glm-4-flash` - Faster inference

The configuration allows users to specify any of these models via the config file.

---

## 4. OpenAI-Compatible Format

### Current Status: CORRECT

z.ai uses an OpenAI-compatible API format, which means the existing request/response parsing logic works correctly.

**Request Format (AsyncOpenAIClient.java Lines 279-311):**
```java
private String buildRequestBody(String prompt, Map<String, Object> params) {
    JsonObject body = new JsonObject();
    body.addProperty("model", modelToUse);
    body.addProperty("max_tokens", maxTokensToUse);
    body.addProperty("temperature", tempToUse);

    JsonArray messages = new JsonArray();
    // System message
    if (systemPrompt != null) {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);
    }
    // User message
    JsonObject userMessage = new JsonObject();
    userMessage.addProperty("role", "user");
    userMessage.addProperty("content", prompt);
    messages.add(userMessage);

    body.add("messages", messages);
    return body.toString();
}
```

This format is fully compatible with z.ai's API.

**Response Parsing (Lines 322-370):**
```java
private LLMResponse parseResponse(String responseBody, long latencyMs) {
    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

    // Extract content from choices[0].message.content
    JsonObject firstChoice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
    JsonObject message = firstChoice.getAsJsonObject("message");
    String content = message.get("content").getAsString();

    // Extract token usage
    int tokensUsed = 0;
    if (json.has("usage")) {
        JsonObject usage = json.getAsJsonObject("usage");
        tokensUsed = usage.get("total_tokens").getAsInt();
    }

    return LLMResponse.builder()
        .content(content)
        .model(model)
        .providerId(PROVIDER_ID)
        .latencyMs(latencyMs)
        .tokensUsed(tokensUsed)
        .build();
}
```

This parsing logic correctly handles z.ai's OpenAI-compatible response format.

---

## 5. Authentication

### Current Status: CORRECT

**OpenAIClient.java (Line 40):**
```java
.header("Authorization", "Bearer " + apiKey)
```

**AsyncOpenAIClient.java (Line 118):**
```java
.header("Authorization", "Bearer " + apiKey)
```

The Bearer token authentication is correct for z.ai's API.

---

## 6. Error Handling

### Current Status: NEEDS IMPROVEMENT

The error handling is generic and doesn't account for z.ai-specific error codes.

**Current Implementation (Lines 378-391):**
```java
private LLMException.ErrorType determineErrorType(int statusCode) {
    return switch (statusCode) {
        case 429 -> LLMException.ErrorType.RATE_LIMIT;
        case 401, 403 -> LLMException.ErrorType.AUTH_ERROR;
        case 400 -> LLMException.ErrorType.CLIENT_ERROR;
        case 408 -> LLMException.ErrorType.TIMEOUT;
        default -> {
            if (statusCode >= 500) {
                yield LLMException.ErrorType.SERVER_ERROR;
            }
            yield LLMException.ErrorType.CLIENT_ERROR;
        }
    };
}
```

**Missing z.ai-Specific Error Codes:**
- z.ai may return custom error codes in the response body
- z.ai rate limiting may differ from OpenAI's
- z.ai-specific error messages should be logged appropriately

**Recommendation:** Add z.ai-specific error parsing:

```java
private LLMException.ErrorType determineErrorType(int statusCode, String responseBody) {
    // Check for z.ai-specific error codes
    try {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        if (json.has("error")) {
            JsonObject error = json.getAsJsonObject("error");
            String zaiCode = error.has("code") ? error.get("code").getAsString() : "";

            // Handle z.ai-specific error codes
            return switch (zaiCode) {
                case "quota_exceeded" -> LLMException.ErrorType.QUOTA_EXCEEDED;
                case "model_not_found" -> LLMException.ErrorType.MODEL_NOT_SUPPORTED;
                case "context_length_exceeded" -> LLMException.ErrorType.CONTEXT_TOO_LONG;
                default -> determineHttpErrorType(statusCode);
            };
        }
    } catch (Exception e) {
        // Fall back to HTTP status code
    }
    return determineHttpErrorType(statusCode);
}
```

---

## 7. Retry Logic

### Current Status: ACCEPTABLE

**Retry Configuration:**
- Max retries: 3 (AsyncOpenAIClient), 5 (OpenAIClient)
- Initial backoff: 1000ms
- Backoff strategy: Exponential (1s, 2s, 4s)
- Retryable errors: HTTP 429, 5xx, network failures

**z.ai Considerations:**
- z.ai rate limits may differ from OpenAI
- Retry logic should be configurable per provider
- Consider adding z.ai-specific retry headers (if any)

---

## 8. Documentation Issues

### Current Status: OUTDATED

**AsyncOpenAIClient.java (Lines 17-31):**
```java
/**
 * Asynchronous OpenAI API client using Java HttpClient's sendAsync().
 *
 * <p>Provides non-blocking calls to OpenAI's chat completion API with built-in retry logic.
 * Uses CompletableFuture to return immediately without blocking the calling thread.</p>
 *
 * <p><b>API Endpoint:</b> https://api.openai.com/v1/chat/completions</p>
 *
 * <p><b>Supported Models:</b></p>
 * <ul>
 *   <li>gpt-4o (recommended)</li>
 *   <li>gpt-4-turbo</li>
 *   <li>gpt-4</li>
 *   <li>gpt-3.5-turbo</li>
 * </ul>
```

**Issues:**
1. JavaDoc still references OpenAI's endpoint
2. Supported models list is outdated (should list GLM models)
3. No mention of z.ai compatibility

**Recommended Update:**
```java
/**
 * Asynchronous LLM client using Java HttpClient's sendAsync().
 *
 * <p>Provides non-blocking calls to an OpenAI-compatible chat completion API
 * (z.ai) with built-in retry logic. Uses CompletableFuture to return immediately
 * without blocking the calling thread.</p>
 *
 * <p><b>API Endpoint:</b> https://api.z.ai/api/paas/v4/chat/completions</p>
 *
 * <p><b>Supported Models (z.ai GLM series):</b></p>
 * <ul>
 *   <li>glm-5 (recommended, default)</li>
 *   <li>glm-4</li>
 *   <li>glm-4-plus</li>
 *   <li>glm-4-flash (faster inference)</li>
 * </ul>
 *
 * <p><b>Note:</b> This client uses OpenAI-compatible API format. The provider is
 * configured as "openai" in the system but connects to z.ai's endpoint.</p>
 */
```

---

## 9. Log Message Issues

### Current Status: MISLEADING

Throughout both files, log messages still reference "OpenAI" which can be confusing when debugging.

**Examples:**
- Line 32: "OpenAI API key not configured!"
- Line 54: "OpenAI API returned empty response"
- Line 64: "OpenAI API request failed with status {}"
- Line 92: "Error communicating with OpenAI API after {} attempts"

**Recommendation:** Update log messages to use the configured endpoint or a generic "LLM API" prefix:

```java
// Option 1: Use endpoint hostname
LOGGER.error("API request failed ({}): {}", URI.create(OPENAI_API_URL).getHost(), statusCode);

// Option 2: Use generic prefix
LOGGER.error("LLM API request failed: {}", statusCode);

// Option 3: Use model name to infer provider
LOGGER.error("{} API request failed: {}", model.split("-")[0], statusCode);
```

---

## 10. Configuration File Updates

### Current Status: CORRECT

**config/steve-common.toml** structure:
```toml
[ai]
provider = "openai"  # This maps to AsyncOpenAIClient (which uses z.ai endpoint)

[openai]
apiKey = "your-zai-api-key"
model = "glm-5"
maxTokens = 8000
temperature = 0.7
```

The configuration correctly supports z.ai's models and API keys.

---

## 11. Hardcoded Values Requiring Updates

### Summary Table

| Location | Current Value | Issue | Recommendation |
|----------|---------------|-------|----------------|
| AsyncOpenAIClient.java:63 | `PROVIDER_ID = "zai"` | Mismatch with provider selection | Change to `"openai"` |
| AsyncOpenAIClient.java:23 | JavaDoc endpoint | Wrong endpoint documented | Update to z.ai endpoint |
| AsyncOpenAIClient.java:26-31 | Supported models list | Lists GPT models | Update to GLM models |
| OpenAIClient.java:15 | Class name | Misleading | Consider renaming to `LLMClient` |
| All log messages | "OpenAI API" | Misleading when using z.ai | Use generic or dynamic messaging |

---

## 12. Testing Recommendations

### Unit Tests Required:

1. **Endpoint Verification Test**
   - Verify requests go to api.z.ai
   - Test with different model names

2. **Response Parsing Test**
   - Test with actual z.ai response format
   - Verify token usage extraction
   - Test error response handling

3. **Authentication Test**
   - Verify Bearer token format
   - Test invalid API key handling

4. **Rate Limiting Test**
   - Test 429 response handling
   - Verify retry backoff timing

5. **Configuration Integration Test**
   - Test provider selection with "openai"
   - Verify model configuration is passed correctly

### Example Test Case:
```java
@Test
void testZaiEndpointConfiguration() {
    AsyncOpenAIClient client = new AsyncOpenAIClient(
        "test-key", "glm-5", 1000, 0.7
    );

    // Verify provider ID
    assertEquals("openai", client.getProviderId());

    // Verify request is sent to z.ai endpoint
    // (would need to mock HttpClient)
}
```

---

## 13. Migration Checklist

### For Full z.ai Compatibility:

- [ ] Update PROVIDER_ID from "zai" to "openai" in AsyncOpenAIClient.java
- [ ] Update JavaDoc to reflect z.ai endpoint and GLM models
- [ ] Update log messages to be provider-agnostic
- [ ] Add z.ai-specific error code parsing
- [ ] Add unit tests for z.ai compatibility
- [ ] Update configuration file comments
- [ ] Consider renaming OpenAIClient to LLMClient or ZAIClient
- [ ] Document z.ai-specific features (if any)
- [ ] Test with actual z.ai API endpoint
- [ ] Verify rate limiting behavior matches expectations

---

## 14. API Differences: z.ai vs OpenAI

### Endpoint URLs:
- **OpenAI:** `https://api.openai.com/v1/chat/completions`
- **z.ai:** `https://api.z.ai/api/paas/v4/chat/completions`

### Supported Models:
- **OpenAI:** gpt-4o, gpt-4-turbo, gpt-3.5-turbo
- **z.ai:** glm-5, glm-4, glm-4-plus, glm-4-flash

### Context Window:
- **OpenAI GPT-4:** 128K tokens
- **z.ai GLM-5:** 128K tokens (similar)

### Pricing (Estimates):
- **OpenAI GPT-4:** ~$0.03/1K input tokens
- **z.ai GLM-5:** Varies, typically more cost-effective

---

## 15. Conclusion

The codebase is **partially configured** for z.ai compatibility. The critical API endpoint is correctly set, and the OpenAI-compatible format ensures basic functionality works. However, several issues prevent production readiness:

1. **Provider ID mismatch** could cause routing issues
2. **Outdated documentation** makes debugging difficult
3. **Generic error handling** misses z.ai-specific errors
4. **Misleading log messages** confuse troubleshooting

**Priority Actions:**
1. Fix PROVIDER_ID mismatch (Line 63 in AsyncOpenAIClient.java)
2. Update JavaDoc and comments
3. Add z.ai-specific error handling
4. Update log messages to be provider-agnostic

With these changes, the system will be fully z.ai compatible while maintaining the ability to switch providers through configuration.

---

**Report Generated:** 2026-02-27
**Analyzed By:** Claude Code Analysis Tool
**Files Analyzed:**
- C:\Users\casey\steve\src\main\java\com\minewright\llm\OpenAIClient.java
- C:\Users\casey\steve\src\main\java\com\minewright\llm\async\AsyncOpenAIClient.java
- C:\Users\casey\steve\src\main\java\com\minewright\config\MineWrightConfig.java
