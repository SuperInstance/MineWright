# LLM Client Resilience Improvements

**Date:** 2026-02-27
**Status:** Analysis & Recommendations
**Priority:** High - Production Readiness

---

## Executive Summary

This document analyzes the current LLM resilience implementation and provides specific code improvements for production readiness. The codebase has a solid foundation with Resilience4j integration, but several critical issues need addressing.

### Current Architecture

```
AsyncOpenAIClient (HTTP client with retry)
         ↓
ResilientLLMClient (decorator with circuit breaker, retry, rate limiter, bulkhead)
         ↓
LLMFallbackHandler (pattern-based fallback)
         ↓
FallbackResponseSystem (template-based responses)
```

### Key Findings

| Area | Status | Issues Found | Severity |
|------|--------|--------------|----------|
| Retry Logic | Partial | Double retry (client + decorator), no jitter | High |
| Circuit Breaker | Good | Config too aggressive (50% threshold) | Medium |
| Timeout Handling | Weak | No timeout propagation, hardcoded values | High |
| Fallback Responses | Mixed | Basic patterns, no context awareness | Medium |
| Error Messages | Good | Detailed but missing key context | Low |
| Thread Safety | Good | Proper use of concurrent collections | Good |

---

## 1. Retry Logic Issues

### Problem: Double Retry Pattern

**Current State:** The code implements retry logic in TWO places:

1. **AsyncOpenAIClient.sendWithRetry()** (lines 145-227)
2. **ResilientLLMClient** wraps with Resilience4j Retry decorator

This causes:
- Unpredictable retry behavior (up to 3 × 3 = 9 attempts)
- Conflicting backoff strategies
- Difficult debugging and monitoring

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\AsyncOpenAIClient.java`

```java
// BEFORE - Current problematic code (lines 145-227)
private CompletableFuture<LLMResponse> sendWithRetry(HttpRequest request, long startTime, int retryCount) {
    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
            // ... handle response ...
            if (isRetryable && retryCount < MAX_RETRIES) {
                long backoffMs = INITIAL_BACKOFF_MS * (1L << retryCount);  // ❌ No jitter
                throw new RetryableException(...);
            }
            // ...
        })
        .exceptionallyCompose(error -> {
            // ❌ Network retry logic duplicates Resilience4j
            if (isNetworkError(cause) && retryCount < MAX_RETRIES) {
                return scheduleRetry(...);
            }
            // ...
        });
}
```

### Fix: Remove Client-Level Retry

**Solution:** Let Resilience4j handle ALL retry logic. The HTTP client should only handle the request/response cycle.

```java
// AFTER - Improved version
private CompletableFuture<LLMResponse> sendWithRetry(HttpRequest request, long startTime, int retryCount) {
    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
            long latencyMs = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                return parseResponse(response.body(), latencyMs);
            }

            // ✅ Only determine error type, let Resilience4j handle retry
            LLMException.ErrorType errorType = determineErrorType(response.statusCode());
            boolean isRetryable = response.statusCode() == 429 || response.statusCode() >= 500;

            LOGGER.error("[openai] API error: status={}, body={}",
                response.statusCode(), truncate(response.body(), 200));

            // ✅ Throw once, let decorator handle retry decision
            throw new LLMException(
                "OpenAI API error: HTTP " + response.statusCode(),
                errorType,
                PROVIDER_ID,
                isRetryable
            );
        })
        .exceptionallyCompose(error -> {
            Throwable cause = error instanceof CompletionException ? error.getCause() : error;

            // ✅ Convert network errors to LLMException for Resilience4j
            if (isNetworkError(cause)) {
                LOGGER.error("[openai] Network error: {}", cause.getMessage());
                throw new LLMException(
                    "OpenAI network error: " + cause.getMessage(),
                    LLMException.ErrorType.NETWORK_ERROR,
                    PROVIDER_ID,
                    true,  // retryable
                    cause
                );
            }

            // ✅ Re-throw LLMException as-is
            if (cause instanceof LLMException) {
                throw (LLMException) cause;
            }

            // ✅ Unknown errors
            throw new LLMException(
                "Unexpected error: " + cause.getMessage(),
                LLMException.ErrorType.NETWORK_ERROR,
                PROVIDER_ID,
                true,
                cause
            );
        });
}
```

### Add Jitter to Retry Backoff

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\ResilienceConfig.java`

```java
// BEFORE (line 133)
.intervalFunction(IntervalFunction.ofExponentialBackoff(RETRY_INITIAL_INTERVAL_MS, 2))

// AFTER - Add jitter to prevent thundering herd
.intervalFunction(IntervalFunction.ofExponentialBackoffRandom(
    RETRY_INITIAL_INTERVAL_MS,
    2,  // multiplier
    0.5  // jitter factor (50% randomness)
))
```

---

## 2. Circuit Breaker Configuration

### Problem: Too Aggressive Thresholds

**Current Issues:**
- 50% failure rate triggers too easily
- Sliding window of 10 is too small for bursty traffic
- 30-second wait may be too long for gaming use case

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\ResilienceConfig.java`

```java
// BEFORE (lines 39-43)
private static final int CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE = 10;
private static final float CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD = 50.0f;
private static final int CIRCUIT_BREAKER_WAIT_DURATION_SECONDS = 30;

// AFTER - Production-ready configuration
private static final int CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE = 20;  // ✅ Larger window
private static final float CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD = 60.0f;  // ✅ Higher threshold
private static final int CIRCUIT_BREAKER_WAIT_DURATION_SECONDS = 20;  // ✅ Faster recovery
private static final int CIRCUIT_BREAKER_HALF_OPEN_CALLS = 5;  // ✅ More test calls
```

### Add Circuit Breaker Metrics Export

```java
// Add to ResilientLLMClient class
/**
 * Returns detailed circuit breaker state for monitoring.
 * Useful for dashboards and health check endpoints.
 */
public CircuitBreakerStateSnapshot getCircuitBreakerState() {
    CircuitBreaker.State state = circuitBreaker.getState();
    CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

    return new CircuitBreakerStateSnapshot(
        providerId,
        state.name(),
        metrics.getFailureRate(),
        metrics.getNumberOfBufferedCalls(),
        metrics.getNumberOfFailedCalls(),
        metrics.getNumberOfSuccessfulCalls(),
        CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE
    );
}

public record CircuitBreakerStateSnapshot(
    String providerId,
    String state,
    float failureRate,
    int bufferedCalls,
    int failedCalls,
    int successfulCalls,
    int slidingWindowSize
) {}
```

---

## 3. Timeout Handling

### Problem: Missing Timeout Propagation

**Current Issues:**
- Hardcoded 30-second timeout in HTTP client (line 120)
- No timeout configuration per provider
- No distinction between connect timeout and read timeout
- No timeout in circuit breaker configuration

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\AsyncOpenAIClient.java`

```java
// BEFORE (lines 100-102, 120)
this.httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))  // ❌ Hardcoded
    .build();
// ...
.timeout(Duration.ofSeconds(30))  // ❌ Hardcoded, no distinction

// AFTER - Configurable timeouts
public AsyncOpenAIClient(String apiKey, String model, int maxTokens, double temperature,
                        Duration connectTimeout, Duration readTimeout) {
    // ... validation ...

    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .connectTimeout(Duration.ofSeconds(5))  // ✅ Separate connect timeout
        .build();

    this.readTimeout = readTimeout;
    // ...
}

// In sendAsync method:
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(OPENAI_API_URL))
    .timeout(readTimeout)  // ✅ Use instance field
    .build();
```

### Add Timeout to Circuit Breaker

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\ResilienceConfig.java`

```java
// Add to createCircuitBreakerConfig()
public static CircuitBreakerConfig createCircuitBreakerConfig() {
    return CircuitBreakerConfig.custom()
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
        .slidingWindowSize(CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE)
        .failureRateThreshold(CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD)
        .waitDurationInOpenState(Duration.ofSeconds(CIRCUIT_BREAKER_WAIT_DURATION_SECONDS))
        .permittedNumberOfCallsInHalfOpenState(CIRCUIT_BREAKER_HALF_OPEN_CALLS)
        // ✅ Add timeout duration - if calls take too long, count as failure
        .slowCallDurationThreshold(Duration.ofSeconds(10))
        .slowCallRateThreshold(50.0f)
        .recordExceptions(IOException.class, TimeoutException.class, LLMException.class)
        .ignoreExceptions(IllegalArgumentException.class)
        .build();
}
```

---

## 4. Fallback Response Improvements

### Problem: Limited Context Awareness

**Current Issues:**
- Pattern matching is too rigid (full string match)
- No learning from user corrections
- No conversation context
- Generic responses for complex queries

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\LLMFallbackHandler.java`

```java
// BEFORE (line 129) - Full string match is too restrictive
if (entry.getKey().matcher(lowerPrompt).matches()) {
    return entry.getValue();
}

// AFTER - Use find() for partial matches
Matcher matcher = entry.getKey().matcher(lowerPrompt);
if (matcher.find()) {
    // ✅ Extract entities from the match
    String matchedText = matcher.group();
    LOGGER.debug("Matched pattern '{}' in prompt", matchedText);

    // ✅ Customize response with matched entities
    return customizeResponse(entry.getValue(), matchedText, lowerPrompt);
}
```

### Add Entity Extraction

```java
/**
 * Customizes fallback response with extracted entities.
 */
private String customizeResponse(String template, String matchedText, String fullPrompt) {
    try {
        JsonObject templateJson = JsonParser.parseString(template).getAsJsonObject();
        JsonArray tasks = templateJson.getAsJsonArray("tasks");

        if (tasks != null && !tasks.isEmpty()) {
            JsonObject firstTask = tasks.get(0).getAsJsonObject();

            // Extract block type from prompt
            if (fullPrompt.contains("cobblestone")) {
                firstTask.addProperty("block", "cobblestone");
            } else if (fullPrompt.contains("oak")) {
                firstTask.addProperty("block", "oak_planks");
            }

            // Extract quantity
            Pattern qtyPattern = Pattern.compile("(\\d+)\\s*(block|item|ore)");
            Matcher qtyMatcher = qtyPattern.matcher(fullPrompt);
            if (qtyMatcher.find()) {
                int quantity = Integer.parseInt(qtyMatcher.group(1));
                JsonObject params = firstTask.getAsJsonObject("parameters");
                if (params == null) {
                    params = new JsonObject();
                    firstTask.add("parameters", params);
                }
                params.addProperty("quantity", quantity);
            }
        }

        return templateJson.toString();
    } catch (Exception e) {
        LOGGER.warn("Failed to customize response, using template", e);
        return template;
    }
}
```

### Add Context-Aware Fallback

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\FallbackResponseSystem.java`

```java
// Add new field to track conversation context
private final Queue<String> recentPrompts = new ConcurrentLinkedQueue<>();
private static final int MAX_PROMPT_HISTORY = 5;

public LLMResponse generateFallback(String systemPrompt, String userPrompt, Throwable error) {
    // ✅ Add to conversation history
    recentPrompts.add(userPrompt);
    if (recentPrompts.size() > MAX_PROMPT_HISTORY) {
        recentPrompts.poll();
    }

    // ✅ Check for repeated prompts (user retrying)
    if (isRepeatedPrompt(userPrompt)) {
        LOGGER.info("Detected repeated prompt, escalating response");
        return generateEscalatedResponse(userPrompt);
    }

    // ... rest of method ...
}

/**
 * Checks if this prompt has been seen recently.
 */
private boolean isRepeatedPrompt(String prompt) {
    return recentPrompts.stream()
        .anyMatch(p -> p.equalsIgnoreCase(prompt));
}

/**
 * Generates a response indicating the need for human intervention.
 */
private LLMResponse generateEscalatedResponse(String prompt) {
    String response = String.format(
        "{\"thoughts\":\"[Fallback] I've tried but cannot process: '%s'. The LLM service is unavailable and my pattern matching cannot help. Please try again later or rephrase your command.\",\"tasks\":[{\"action\":\"wait\",\"duration\":10,\"fallback_mode\":\"escalated\"}]}",
        prompt
    );

    return LLMResponse.builder()
        .content(response)
        .model("fallback-escalated")
        .providerId("fallback")
        .latencyMs(1)
        .tokensUsed(0)
        .fromCache(false)
        .build();
}
```

---

## 5. Error Message Improvements

### Problem: Missing Debug Context

**Current Issues:**
- Error messages don't include request IDs
- No correlation between retries
- Missing timing information
- Hard to trace issues in logs

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\AsyncOpenAIClient.java`

```java
// BEFORE (line 123)
LOGGER.debug("[openai] Sending async request (prompt length: {} chars)", prompt.length());

// AFTER - Add request correlation
private final AtomicLong requestIdGenerator = new AtomicLong(0);

public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
    long requestId = requestIdGenerator.incrementAndGet();
    long startTime = System.currentTimeMillis();

    LOGGER.debug("[openai][REQ-{}] Sending async request (prompt length: {}, model: {})",
        requestId, prompt.length(), model);

    String requestBody = buildRequestBody(prompt, params);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(OPENAI_API_URL))
        .header("X-Request-ID", String.valueOf(requestId))  // ✅ Propagate to API
        // ... rest of build ...

    return sendWithRetry(request, startTime, 0, requestId);  // ✅ Pass through
}

// Update all log statements to include [REQ-{requestId}]
LOGGER.warn("[openai][REQ-{}] Retryable error (status={}, attempt {}/{}), retrying in {}ms",
    requestId, response.statusCode(), nextRetryCount, MAX_RETRIES, backoffMs);
```

### Add Structured Error Response

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\LLMException.java`

```java
// Add new fields
private final String requestId;
private final long timestamp;
private final int attemptNumber;

public LLMException(String message, ErrorType errorType, String providerId,
                    boolean retryable, String requestId, int attemptNumber) {
    super(message);
    this.errorType = errorType;
    this.providerId = providerId;
    this.retryable = retryable;
    this.requestId = requestId;
    this.timestamp = System.currentTimeMillis();
    this.attemptNumber = attemptNumber;
}

// Add to toString()
@Override
public String toString() {
    return String.format("LLMException{requestId=%s, errorType=%s, providerId='%s', " +
                        "retryable=%s, attempt=%d, age=%dms, message='%s'}",
        requestId, errorType, providerId, retryable, attemptNumber,
        System.currentTimeMillis() - timestamp, getMessage());
}
```

---

## 6. Additional Production Improvements

### A. Add Request Prioritization

```java
/**
 * Priority levels for LLM requests.
 */
public enum RequestPriority {
    CRITICAL,   // Combat, immediate danger
    HIGH,       // User commands
    MEDIUM,     // Background tasks
    LOW         // Analytics, logging
}

// Update AsyncLLMClient interface
CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params,
                                        RequestPriority priority);
```

### B. Add Request Queuing

```java
/**
 * Queued request with priority and timestamp.
 */
private static class QueuedRequest {
    final String prompt;
    final Map<String, Object> params;
    final RequestPriority priority;
    final long timestamp;
    final CompletableFuture<LLMResponse> future;

    QueuedRequest(String prompt, Map<String, Object> params, RequestPriority priority) {
        this.prompt = prompt;
        this.params = params;
        this.priority = priority;
        this.timestamp = System.nanoTime();
        this.future = new CompletableFuture<>();
    }
}

// PriorityBlockingQueue for fair ordering
private final PriorityBlockingQueue<QueuedRequest> requestQueue =
    new PriorityBlockingQueue<>(100,
        Comparator.comparing((QueuedRequest r) -> r.priority)
                 .thenComparingLong(r -> r.timestamp));
```

### C. Add Metrics Collection

```java
/**
 * Metrics for LLM client operations.
 */
public static class ClientMetrics {
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong retriedRequests = new AtomicLong(0);
    private final AtomicLong fallbackRequests = new AtomicLong(0);
    private final LongAdder totalLatencyMs = new LongAdder();

    public void recordRequest(long latencyMs, boolean success, boolean retried, boolean fallback) {
        totalRequests.incrementAndGet();
        if (success) successfulRequests.incrementAndGet();
        else failedRequests.incrementAndGet();
        if (retried) retriedRequests.incrementAndGet();
        if (fallback) fallbackRequests.incrementAndGet();
        totalLatencyMs.add(latencyMs);
    }

    public double getSuccessRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) successfulRequests.get() / total : 0.0;
    }

    public double getAverageLatencyMs() {
        long total = totalRequests.get();
        return total > 0 ? (double) totalLatencyMs.sum() / total : 0.0;
    }

    // Export as JSON for monitoring
    public String toJson() {
        return String.format(
            "{\"totalRequests\":%d,\"successful\":%d,\"failed\":%d,\"retried\":%d," +
            "\"fallback\":%d,\"successRate\":%.2f,\"avgLatencyMs\":%.2f}",
            totalRequests.get(), successfulRequests.get(), failedRequests.get(),
            retriedRequests.get(), fallbackRequests.get(),
            getSuccessRate(), getAverageLatencyMs()
        );
    }
}
```

### D. Add Health Check Endpoint

```java
/**
 * Performs a health check on the LLM client.
 */
public HealthCheckResult performHealthCheck() {
    HealthCheckResult result = new HealthCheckResult();
    result.providerId = getProviderId();
    result.timestamp = System.currentTimeMillis();

    // Check circuit breaker
    CircuitBreaker.State cbState = circuitBreaker.getState();
    result.circuitBreakerState = cbState.name();
    result.isCircuitBreakerHealthy = cbState != CircuitBreaker.State.OPEN;

    // Check recent metrics
    CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
    result.failureRate = metrics.getFailureRate();
    result.bufferedCalls = metrics.getNumberOfBufferedCalls();

    // Overall health
    result.isHealthy = result.isCircuitBreakerHealthy && result.failureRate < 80.0f;

    // Recommendations
    if (result.failureRate > 70.0f) {
        result.recommendations.add("High failure rate detected - consider switching providers");
    }
    if (!result.isCircuitBreakerHealthy) {
        result.recommendations.add("Circuit breaker open - requests will use fallback");
    }
    if (rateLimiter.getMetrics().getAvailablePermissions() == 0) {
        result.recommendations.add("Rate limiter saturated - requests are being throttled");
    }

    return result;
}

public static class HealthCheckResult {
    public String providerId;
    public long timestamp;
    public String circuitBreakerState;
    public boolean isCircuitBreakerHealthy;
    public float failureRate;
    public int bufferedCalls;
    public boolean isHealthy;
    public List<String> recommendations = new ArrayList<>();

    public String toJson() {
        return String.format(
            "{\"providerId\":\"%s\",\"timestamp\":%d,\"circuitBreaker\":\"%s\"," +
            "\"healthy\":%s,\"failureRate\":%.2f,\"recommendations\":%s}",
            providerId, timestamp, circuitBreakerState, isHealthy,
            failureRate, recommendations
        );
    }
}
```

---

## 7. Testing Strategy

### Unit Tests for Retry Logic

```java
@Test
public void testRetryWithJitter() {
    // Given
    AsyncOpenAIClient client = new AsyncOpenAIClient(apiKey, model, 1000, 0.7);
    AtomicInteger attemptCount = new AtomicInteger(0);

    // When - Simulate failures with jitter
    CompletableFuture<LLMResponse> future = client.sendAsync("test", Map.of());

    // Then - Verify retry attempts with varying delays
    // (implementation depends on test setup)
}

@Test
public void testCircuitBreakerOpensAtThreshold() {
    // Given
    ResilienceConfig config = new ResilienceConfig();

    // When - Trigger 60% failure rate
    for (int i = 0; i < 20; i++) {
        if (i < 12) {
            // Simulate failure
        } else {
            // Simulate success
        }
    }

    // Then - Circuit should be OPEN
    assertEquals(CircuitBreaker.State.OPEN, client.getCircuitBreakerState());
}
```

### Integration Tests

```java
@Test
public void testEndToEndResilience() {
    // Given
    AsyncLLMClient client = new ResilientLLMClient(
        new AsyncOpenAIClient(...),
        new LLMCache(),
        new LLMFallbackHandler()
    );

    // When - Simulate network failure
    // Then - Verify fallback is used

    // When - Network recovers
    // Then - Verify normal operation resumes
}
```

---

## Implementation Priority

### Phase 1: Critical Fixes (Week 1)
1. ✅ Remove double retry logic
2. ✅ Add jitter to retry backoff
3. ✅ Fix timeout handling
4. ✅ Add request correlation IDs

### Phase 2: Configuration Tuning (Week 2)
1. ✅ Adjust circuit breaker thresholds
2. ✅ Add slow call detection
3. ✅ Per-provider timeout configuration

### Phase 3: Enhanced Fallbacks (Week 3)
1. ✅ Improve pattern matching with find()
2. ✅ Add entity extraction
3. ✅ Add conversation context
4. ✅ Add escalation for repeated failures

### Phase 4: Observability (Week 4)
1. ✅ Add metrics collection
2. ✅ Add health check endpoint
3. ✅ Add circuit breaker state export
4. ✅ Add structured logging

---

## Monitoring Recommendations

### Key Metrics to Track

1. **Request Metrics**
   - Total requests per minute
   - Success rate (target: >95%)
   - Average latency (target: <2s)
   - P95, P99 latency

2. **Resilience Metrics**
   - Circuit breaker state transitions
   - Circuit breaker open time
   - Retry rate (target: <10%)
   - Fallback rate (target: <5%)

3. **Cache Metrics**
   - Cache hit rate (target: >40%)
   - Cache size
   - Eviction rate

### Alerting Thresholds

```yaml
alerts:
  - name: HighFailureRate
    condition: failure_rate > 50%
    duration: 5m
    severity: warning

  - name: CircuitBreakerOpen
    condition: circuit_breaker_state == OPEN
    duration: 1m
    severity: critical

  - name: HighFallbackRate
    condition: fallback_rate > 20%
    duration: 10m
    severity: warning

  - name: HighLatency
    condition: p99_latency > 10s
    duration: 5m
    severity: warning
```

---

## Conclusion

The current LLM resilience implementation has a solid foundation but needs refinement for production use. The main issues are:

1. **Double retry pattern** causing unpredictable behavior
2. **Aggressive circuit breaker** thresholds
3. **Missing timeout** propagation
4. **Rigid fallback** system

The proposed improvements address these issues while maintaining the existing architecture. Implementation should follow the phased approach to minimize disruption while gradually improving reliability.

**Estimated Effort:** 4 weeks
**Risk Level:** Medium (refactoring retry logic)
**ROI:** High - improved reliability and debuggability

---

## References

- **Files Analyzed:**
  - `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\AsyncOpenAIClient.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\ResilientLLMClient.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\ResilienceConfig.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\LLMFallbackHandler.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\llm\FallbackResponseSystem.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\LLMException.java`
  - `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\LLMCache.java`

- **Related Documentation:**
  - Resilience4j Documentation: https://resilience4j.readme.io/docs
  - Java HttpClient Guide: https://openjdk.org/groups/net/httpclient/intro.html
  - Circuit Breaker Pattern: https://martinfowler.com/bliki/CircuitBreaker.html
