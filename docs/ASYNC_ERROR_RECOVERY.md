# Async Error Recovery Patterns for MineWright

## Overview

This document analyzes the current error handling implementation in MineWright's async LLM infrastructure and provides recommended patterns for robust error recovery. The project uses `CompletableFuture` for non-blocking LLM calls with Resilience4j integration for fault tolerance.

**Project Context:** MineWright (Minecraft mod) uses async LLM calls to plan agent actions without blocking the game thread. The architecture is built around:

- **AsyncLLMClient** interface with implementations for OpenAI, Groq, and Gemini
- **ResilientLLMClient** wrapper with circuit breaker, retry, rate limiting, and bulkhead patterns
- **LLMCache** for response caching (40-60% hit rate)
- **LLMFallbackHandler** for pattern-based fallback responses
- **ActionExecutor** for non-blocking command processing

---

## Table of Contents

1. [Current Implementation Analysis](#current-implementation-analysis)
2. [CompletableFuture Error Handling Patterns](#completablefuture-error-handling-patterns)
3. [Circuit Breaker Integration](#circuit-breaker-integration)
4. [Retry Strategies](#retry-strategies)
5. [Graceful Degradation](#graceful-degradation)
6. [Fallback Behaviors](#fallback-behaviors)
7. [Testing Strategy](#testing-strategy)
8. [Recommended Improvements](#recommended-improvements)

---

## Current Implementation Analysis

### AsyncOpenAIClient Error Handling

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\AsyncOpenAIClient.java`

**Strengths:**
- Comprehensive retry logic with exponential backoff (1s, 2s, 4s)
- Typed exceptions via `LLMException` with `ErrorType` enum
- Retryable vs non-retryable error classification
- Network error detection for connection failures
- Scheduled retry mechanism using dedicated scheduler thread

**Current Retry Behavior:**
```java
// Max retries: 3
// Backoff: Exponential (1s, 2s, 4s)
// Retryable: HTTP 429 (rate limit), HTTP 5xx (server errors), network failures
// Non-retryable: HTTP 4xx client errors (except 429)
```

**Key Pattern - ExceptionalCompose:**
```java
.exceptionallyCompose(error -> {
    Throwable cause = error instanceof CompletionException ? error.getCause() : error;

    if (cause instanceof RetryableException) {
        return scheduleRetry(request, startTime, re.backoffMs, re.nextRetryCount);
    }

    if (isNetworkError(cause) && retryCount < MAX_RETRIES) {
        return scheduleRetry(request, startTime, backoffMs, nextRetryCount);
    }

    return CompletableFuture.failedFuture(wrapInLLMException(cause));
});
```

**Analysis:** The use of `exceptionallyCompose` is correct - it allows returning a new `CompletableFuture` for retries, unlike `exceptionally` which can only return a value.

### TaskPlanner Error Handling

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\TaskPlanner.java`

**Strengths:**
- Non-blocking async API with `planTasksAsync()`
- Batching support for rate limit management
- Proper exception handling in `executeAsyncRequest()`
- Null-safe error handling with user feedback

**Current Pattern:**
```java
return client.sendAsync(userPrompt, params)
    .thenApply(response -> {
        // Parse and validate response
        ResponseParser.ParsedResponse parsed = ResponseParser.parseAIResponse(content);
        return parsed;
    })
    .exceptionally(throwable -> {
        LOGGER.error("[Async] Error planning tasks: {}", throwable.getMessage());
        return null;  // Returns null on error
    });
```

**Issue:** Returning `null` on error forces callers to check for null. A better pattern would be to return a `Result` type or propagate the error.

### ResilientLLMClient Error Handling

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\ResilientLLMClient.java`

**Strengths:**
- Full Resilience4j integration (circuit breaker, retry, rate limiter, bulkhead)
- Event listeners for observability
- Fallback handler integration
- Cache-first strategy for performance

**Resilience Chain:**
```java
// Order: RateLimiter -> Bulkhead -> CircuitBreaker -> Retry
Supplier<CompletableFuture<LLMResponse>> decoratedSupplier = decorateWithResilience(asyncSupplier);

return decoratedSupplier.get()
    .thenApply(response -> {
        cache.put(prompt, model, providerId, response);
        return response;
    })
    .exceptionally(throwable -> {
        return fallbackHandler.generateFallback(prompt, cause);
    });
```

**Configuration (ResilienceConfig.java):**
```java
// Circuit Breaker
- Sliding window: 10 calls
- Failure threshold: 50%
- Wait duration: 30 seconds
- Half-open calls: 3

// Retry
- Max attempts: 3
- Initial interval: 1000ms
- Backoff: Exponential (2x)

// Rate Limiter
- Limit: 10 req/min
- Timeout: 5 seconds

// Bulkhead
- Max concurrent: 5
- Wait duration: 10 seconds
```

### ActionExecutor Error Handling

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`

**Strengths:**
- Non-blocking command processing
- Proper lifecycle management (planning flags)
- User feedback via GUI and voice
- Cancellation handling

**Current Pattern:**
```java
public void processNaturalLanguageCommand(String command) {
    if (isPlanning) {
        sendToGUI(foreman.getSteveName(), "Hold on, I'm still thinking...");
        return;
    }

    this.isPlanning = true;
    this.pendingCommand = command;
    planningFuture = getTaskPlanner().planTasksAsync(foreman, command);
}

public void tick() {
    if (isPlanning && planningFuture != null && planningFuture.isDone()) {
        try {
            ResponseParser.ParsedResponse response = planningFuture.get();
            // Process response...
        } catch (CancellationException e) {
            sendToGUI(foreman.getSteveName(), "Planning cancelled.");
        } catch (Exception e) {
            sendToGUI(foreman.getSteveName(), "Oops, something went wrong!");
        } finally {
            isPlanning = false;
            planningFuture = null;
        }
    }
}
```

**Issue:** Uses `planningFuture.get()` which is a blocking call in the tick method. Since `isDone()` is checked first, this should be non-blocking, but it's still not ideal.

---

## CompletableFuture Error Handling Patterns

### 1. Exceptionally vs ExceptionallyCompose

**Use `exceptionally` when:**
- You can recover with a static value or cached response
- You don't need to perform async operations for recovery

```java
// GOOD: Static fallback
client.sendAsync(prompt, params)
    .exceptionally(error -> {
        return LLMResponse.builder()
            .content("{ \"tasks\": [{\"action\": \"wait\"}] }")
            .providerId("fallback")
            .build();
    });
```

**Use `exceptionallyCompose` when:**
- You need to perform async operations for recovery (e.g., retry, fallback API)
- You need to return a new `CompletableFuture`

```java
// GOOD: Async retry
client.sendAsync(prompt, params)
    .exceptionallyCompose(error -> {
        if (isRetryable(error)) {
            return retryWithBackoff(prompt, params);
        }
        return fallbackToCachedResponse(prompt);
    });
```

### 2. Handle Pattern

**Use `handle` when:**
- You need to handle both success and error cases uniformly
- You want to transform errors into successful results

```java
// Pattern: Convert error to success with error indicator
client.sendAsync(prompt, params)
    .handle((response, throwable) -> {
        if (throwable != null) {
            return LLMResponse.builder()
                .content("{ \"error\": \"" + throwable.getMessage() + "\" }")
                .providerId("error-handler")
                .build();
        }
        return response;
    });
```

### 3. WhenComplete Pattern

**Use `whenComplete` when:**
- You need to perform cleanup regardless of success/failure
- You want to log metrics or update state

```java
// Pattern: Metrics logging
client.sendAsync(prompt, params)
    .whenComplete((response, throwable) -> {
        long duration = System.currentTimeMillis() - startTime;
        metrics.recordLatency(duration);

        if (throwable != null) {
            metrics.recordError(throwable.getClass().getSimpleName());
        } else {
            metrics.recordSuccess(response.getTokensUsed());
        }
    });
```

### 4. Composition Patterns

**Recommended composition order:**
```java
client.sendAsync(prompt, params)
    .thenApply(this::parseResponse)           // Transform success
    .thenApply(this::validateResponse)         // Validate
    .exceptionallyCompose(this::handleRetry)   // Async error recovery
    .exceptionally(this::handleFallback)       // Static fallback
    .whenComplete(this::recordMetrics);        // Cleanup/logging
```

---

## Circuit Breaker Integration

### Current Implementation

Resilience4j circuit breaker is configured but **NOT ACTIVELY USED** due to Forge classloading issues. The comment in `TaskPlanner.java` states:

```java
// Initialize async clients directly (no resilience wrapper due to Forge classloading issues)
this.asyncOpenAIClient = new AsyncOpenAIClient(apiKey, model, maxTokens, temperature);
```

### Recommended Pattern for Circuit Breaker

**Option 1: Direct Resilience4j Integration (if classloading resolved)**

```java
// Create circuit breaker registry
CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(
    CircuitBreakerConfig.custom()
        .slidingWindowSize(10)
        .failureRateThreshold(50)
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .build()
);

CircuitBreaker circuitBreaker = registry.circuitBreaker("openai");

// Decorate async call
Supplier<CompletableFuture<LLMResponse>> supplier = () -> client.sendAsync(prompt, params);

Supplier<CompletableFuture<LLMResponse>> decorated =
    CircuitBreaker.decorateSupplier(circuitBreaker, supplier);

return decorated.get();
```

**Option 2: Manual Circuit Breaker (Recommended for Forge)**

Create a simple circuit breaker implementation to avoid classloading issues:

```java
public class SimpleCircuitBreaker {
    private enum State { CLOSED, OPEN, HALF_OPEN }

    private volatile State state = State.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final int threshold = 5;
    private final long waitTimeMs = 30000;
    private volatile long lastFailureTime;

    public boolean allowRequest() {
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime > waitTimeMs) {
                state = State.HALF_OPEN;
                return true;
            }
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        failureCount.set(0);
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
        }
    }

    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();
        if (failures >= threshold) {
            state = State.OPEN;
        }
    }

    public State getState() {
        return state;
    }
}
```

**Integration with AsyncOpenAIClient:**

```java
private final SimpleCircuitBreaker circuitBreaker = new SimpleCircuitBreaker();

@Override
public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
    if (!circuitBreaker.allowRequest()) {
        return CompletableFuture.failedFuture(
            new LLMException("Circuit breaker is OPEN",
                LLMException.ErrorType.CIRCUIT_OPEN,
                PROVIDER_ID,
                false)
        );
    }

    return sendWithRetry(request, startTime, 0)
        .whenComplete((response, error) -> {
            if (error != null) {
                circuitBreaker.recordFailure();
            } else {
                circuitBreaker.recordSuccess();
            }
        });
}
```

---

## Retry Strategies

### Current Retry Implementation

**AsyncOpenAIClient:**
- Custom retry with `RetryableException`
- Scheduled executor for delayed retries
- Exponential backoff (1s, 2s, 4s)

**ResilienceConfig:**
- Resilience4j retry configuration
- Exponential backoff with custom predicate
- Max 3 attempts

### Recommended Retry Patterns

### 1. Exponential Backoff with Jitter

```java
public CompletableFuture<LLMResponse> sendWithJitter(String prompt, Map<String, Object> params) {
    return sendWithRetry(prompt, params, 0, 0);
}

private CompletableFuture<LLMResponse> sendWithRetry(
    String prompt,
    Map<String, Object> params,
    int attempt,
    long delayMs
) {
    if (delayMs > 0) {
        // Add jitter: delayMs * (0.5 to 1.5)
        long jitteredDelay = (long) (delayMs * (0.5 + Math.random()));
        try {
            Thread.sleep(jitteredDelay);
        } catch (InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    return sendRequest(prompt, params)
        .exceptionallyCompose(error -> {
            if (attempt < MAX_RETRIES && isRetryable(error)) {
                long nextDelay = (long) (Math.pow(2, attempt) * 1000);
                return sendWithRetry(prompt, params, attempt + 1, nextDelay);
            }
            return CompletableFuture.failedFuture(error);
        });
}
```

### 2. Retry with Context Preservation

```java
private static class RetryContext {
    final String prompt;
    final Map<String, Object> params;
    final long startTime;
    final List<Throwable> errors = new ArrayList<>();

    RetryContext(String prompt, Map<String, Object> params) {
        this.prompt = prompt;
        this.params = params;
        this.startTime = System.currentTimeMillis();
    }

    void addError(Throwable error) {
        errors.add(error);
    }

    String getErrorSummary() {
        return errors.stream()
            .map(Throwable::getMessage)
            .collect(Collectors.joining(", "));
    }
}

public CompletableFuture<LLMResponse> sendWithContext(String prompt, Map<String, Object> params) {
    RetryContext context = new RetryContext(prompt, params);
    return sendWithContext(context, 0);
}

private CompletableFuture<LLMResponse> sendWithContext(RetryContext context, int attempt) {
    return sendRequest(context.prompt, context.params)
        .thenApply(response -> {
            LOGGER.info("Success after {} attempts", attempt);
            return response;
        })
        .exceptionallyCompose(error -> {
            context.addError(error);

            if (attempt < MAX_RETRIES && isRetryable(error)) {
                LOGGER.warn("Attempt {} failed: {}. Retrying...",
                    attempt, error.getMessage());
                return sendWithContext(context, attempt + 1);
            }

            LOGGER.error("Failed after {} attempts: {}",
                attempt, context.getErrorSummary());
            return CompletableFuture.failedFuture(error);
        });
}
```

### 3. Selective Retry by Error Type

```java
private boolean shouldRetry(Throwable error, int attempt) {
    if (attempt >= MAX_RETRIES) {
        return false;
    }

    // Network errors - always retry
    if (error instanceof ConnectException ||
        error instanceof SocketTimeoutException ||
        error instanceof HttpTimeoutException) {
        return true;
    }

    // LLM exceptions - check type
    if (error instanceof LLMException llmError) {
        return switch (llmError.getErrorType()) {
            case RATE_LIMIT, TIMEOUT, NETWORK_ERROR, SERVER_ERROR -> true;
            case CIRCUIT_OPEN, INVALID_RESPONSE, AUTH_ERROR, CLIENT_ERROR -> false;
        };
    }

    // Unknown errors - don't retry
    return false;
}
```

---

## Graceful Degradation

### Degradation Levels

Implement a multi-level degradation strategy:

**Level 1: Cache Hit**
- Serve from cache (fastest)
- 0 latency, 0 API cost
- 40-60% hit rate achievable

**Level 2: Fallback to Secondary Provider**
- Try backup LLM provider (e.g., Groq if OpenAI fails)
- Minimal latency impact
- Spreads load across providers

**Level 3: Pattern-Based Fallback**
- Use `LLMFallbackHandler` for pattern matching
- Recognizes common commands (mine, build, attack, follow)
- Instant response, works offline

**Level 4: Safe Default Action**
- Return "wait" or "idle" action
- Agent remains functional but passive
- Better than crashing or hanging

### Implementation Pattern

```java
public CompletableFuture<LLMResponse> sendWithDegradation(String prompt, Map<String, Object> params) {
    String model = (String) params.getOrDefault("model", "unknown");
    String providerId = getProviderId();

    // Level 1: Check cache
    Optional<LLMResponse> cached = cache.get(prompt, model, providerId);
    if (cached.isPresent()) {
        return CompletableFuture.completedFuture(cached.get());
    }

    // Level 2: Try primary provider
    return sendAsync(prompt, params)
        .exceptionallyCompose(primaryError -> {
            LOGGER.warn("Primary provider failed: {}", primaryError.getMessage());

            // Level 3: Try fallback provider
            return fallbackProvider.sendAsync(prompt, params)
                .exceptionallyCompose(secondaryError -> {
                    LOGGER.warn("Secondary provider failed: {}", secondaryError.getMessage());

                    // Level 4: Pattern-based fallback
                    LLMResponse patternResponse = fallbackHandler.generateFallback(prompt, secondaryError);
                    return CompletableFuture.completedFuture(patternResponse);
                });
        })
        .thenApply(response -> {
            // Cache successful response (from any source)
            cache.put(prompt, model, providerId, response);
            return response;
        });
}
```

---

## Fallback Behaviors

### Current Implementation: LLMFallbackHandler

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\LLMFallbackHandler.java`

**Current Patterns:**
```java
// Mining: "mine", "dig", "collect", "gather"
Pattern.compile("(?i).*(mine|dig|collect|gather|ore).*")

// Building: "build", "construct", "create"
Pattern.compile("(?i).*(build|construct|create).*(house|home|shelter).*")

// Combat: "attack", "fight", "kill"
Pattern.compile("(?i).*(attack|fight|kill).*")

// Follow: "follow", "come", "with me"
Pattern.compile("(?i).*(follow|come|here|with me).*")

// Movement: "go to", "move to", "walk"
Pattern.compile("(?i).*(go to|move to|walk to).*")

// Stop: "stop", "halt", "cancel", "wait"
Pattern.compile("(?i).*(stop|halt|cancel|wait).*")
```

### Recommended Enhancements

### 1. Context-Aware Fallbacks

```java
public LLMResponse generateFallback(String prompt, Throwable error, ForemanEntity foreman) {
    // Get world context for smarter fallbacks
    BlockState nearbyBlock = getNearestBlock(foreman);
    EntityType nearbyEntity = getNearestHostile(foreman);

    // Context-aware pattern matching
    if (prompt.toLowerCase().contains("mine")) {
        if (nearbyBlock != null) {
            return createMineResponse(nearbyBlock.getBlock().getRegistryName());
        }
        return createExploreResponse();  // No resources nearby, explore
    }

    if (prompt.toLowerCase().contains("attack")) {
        if (nearbyEntity != null) {
            return createAttackResponse(nearbyEntity.getEncodeId());
        }
        return createPatrolResponse();  // No enemies nearby, patrol
    }

    return createDefaultResponse();
}
```

### 2. Cached Fallbacks

```java
public class CachedFallbackHandler {
    private final LoadingCache<String, LLMResponse> fallbackCache;

    public CachedFallbackHandler() {
        this.fallbackCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(key -> generateFallbackUncached(key));
    }

    public LLMResponse generateFallback(String prompt, Throwable error) {
        String key = generateKey(prompt, error);
        return fallbackCache.get(key);
    }

    private LLMResponse generateFallbackUncached(String key) {
        // Expensive pattern matching or API call
        return doPatternMatch(key);
    }
}
```

### 3. Rule-Based Actions

```java
public class RuleBasedFallbackHandler {
    private final List<Rule> rules = List.of(
        new MiningRule(),
        new BuildingRule(),
        new CombatRule(),
        new FollowRule(),
        new DefaultRule()
    );

    public LLMResponse generateFallback(String prompt, Throwable error) {
        return rules.stream()
            .filter(rule -> rule.matches(prompt))
            .findFirst()
            .orElse(new DefaultRule())
            .execute(prompt, error);
    }

    interface Rule {
        boolean matches(String prompt);
        LLMResponse execute(String prompt, Throwable error);
    }

    static class MiningRule implements Rule {
        @Override
        public boolean matches(String prompt) {
            return prompt.toLowerCase().matches(".*(mine|dig|collect).*");
        }

        @Override
        public LLMResponse execute(String prompt, Throwable error) {
            // Extract specific block type from prompt
            String block = extractBlockType(prompt).orElse("iron_ore");
            return LLMResponse.builder()
                .content(String.format(
                    "{\"thoughts\":\"[Fallback] Mining %s\",\"tasks\":[{\"action\":\"mine\",\"target\":\"%s\",\"quantity\":10}]}",
                    block, block))
                .providerId("fallback-rule")
                .build();
        }
    }
}
```

---

## Testing Strategy

### Unit Testing

### 1. Error Recovery Tests

```java
@Test
public void testRetryOnNetworkFailure() {
    // Mock HTTP client that fails twice, then succeeds
    HttpClient mockClient = mock(HttpClient.class);
    when(mockClient.sendAsync(any(), any()))
        .thenReturn(failedFuture(new ConnectException("Connection refused")))
        .thenReturn(failedFuture(new ConnectException("Connection refused")))
        .thenReturn(completedFuture(mockResponse(200, "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}")));

    AsyncOpenAIClient client = new AsyncOpenAIClient("test-key", "gpt-4", 1000, 0.7);
    LLMResponse response = client.sendAsync("test", Map.of()).join();

    assertNotNull(response);
    verify(mockClient, times(3)).sendAsync(any(), any());
}

@Test
public void testCircuitBreakerOpensAfterThreshold() {
    SimpleCircuitBreaker cb = new SimpleCircuitBreaker();

    // Record 5 failures
    for (int i = 0; i < 5; i++) {
        assertTrue(cb.allowRequest());
        cb.recordFailure();
    }

    // Circuit should be open
    assertEquals(State.OPEN, cb.getState());
    assertFalse(cb.allowRequest());
}

@Test
public void testCacheFallbackOnProviderFailure() {
    LLMCache cache = mock(LLMCache.class);
    when(cache.get(any(), any(), any()))
        .thenReturn(Optional.of(mockResponse));

    ResilientLLMClient client = new ResilientLLMClient(
        mock(AsyncLLMClient.class),
        cache,
        mock(LLMFallbackHandler.class)
    );

    // Should return cached response without calling provider
    LLMResponse response = client.sendAsync("test", Map.of()).join();
    assertNotNull(response);
    verify(cache).get(any(), any(), any());
}
```

### 2. Fallback Handler Tests

```java
@Test
public void testPatternMatching() {
    LLMFallbackHandler handler = new LLMFallbackHandler();

    LLMResponse response = handler.generateFallback("mine 10 iron ore", null);
    assertTrue(response.getContent().contains("mine"));
    assertTrue(response.getContent().contains("iron_ore"));
}

@Test
public void testDefaultFallback() {
    LLMFallbackHandler handler = new LLMFallbackHandler();

    LLMResponse response = handler.generateFallback("xyz random gibberish", null);
    assertTrue(response.getContent().contains("wait"));
}

@Test
public void testContextAwareFallback() {
    ForemanEntity foreman = mock(ForemanEntity.class);
    when(foreman.getNearestBlock()).thenReturn(Blocks.IRON_ORE);

    ContextAwareFallbackHandler handler = new ContextAwareFallbackHandler();
    LLMResponse response = handler.generateFallback("mine something", null, foreman);

    assertTrue(response.getContent().contains("iron_ore"));
}
```

### Integration Testing

### 3. End-to-End Error Scenarios

```java
@Test
public void testFullErrorRecoveryFlow() {
    // Setup: Circuit breaker open, cache miss, fallback available
    SimpleCircuitBreaker cb = new SimpleCircuitBreaker();
    cb.recordFailure(); // 5 times to open circuit

    TaskPlanner planner = new TaskPlanner();

    // Test: Command should fall back to pattern handler
    CompletableFuture<ParsedResponse> future = planner.planTasksAsync(foreman, "mine iron");

    ParsedResponse response = future.join();
    assertNotNull(response);
    assertTrue(response.getPlan().contains("[Fallback]"));
}
```

### Load Testing

### 4. Concurrency and Stress Tests

```java
@Test
public void testConcurrentRequestsUnderCircuitBreaker() {
    SimpleCircuitBreaker cb = new SimpleCircuitBreaker();
    ExecutorService executor = Executors.newFixedThreadPool(20);

    // Send 100 concurrent requests
    List<CompletableFuture<LLMResponse>> futures = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        futures.add(CompletableFuture.supplyAsync(() -> {
            if (!cb.allowRequest()) {
                throw new RuntimeException("Circuit open");
            }
            // Simulate 50% failure rate
            if (Math.random() < 0.5) {
                cb.recordFailure();
                throw new RuntimeException("Simulated failure");
            }
            cb.recordSuccess();
            return mockResponse;
        }, executor));
    }

    // Verify circuit breaker behavior
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // Circuit should be open after many failures
    // Verify metrics...
}
```

### Chaos Testing

### 5. Fault Injection Tests

```java
@Test
public void testChaosScenario() {
    FaultInjectionClient client = new FaultInjectionClient();

    // Inject random failures: 30% network errors, 20% rate limits, 10% timeouts
    client.setFaultPolicy(FaultPolicy.builder()
        .withNetworkErrorRate(0.3)
        .withRateLimitRate(0.2)
        .withTimeoutRate(0.1)
        .build());

    // Run 1000 requests
    AtomicInteger successCount = new AtomicInteger();
    for (int i = 0; i < 1000; i++) {
        client.sendAsync("test", Map.of())
            .thenAccept(response -> successCount.incrementAndGet())
            .exceptionally(error -> {
                // Expected due to chaos
                return null;
            })
            .join();
    }

    // Verify system stability: >90% success with fallbacks
    assertTrue(successCount.get() > 900);
}
```

---

## Recommended Improvements

### Priority 1: Fix ActionExecutor Blocking Issue

**Issue:** `planningFuture.get()` is called in `tick()` which is technically blocking.

**Fix:**
```java
// Current (blocking):
if (isPlanning && planningFuture != null && planningFuture.isDone()) {
    ResponseParser.ParsedResponse response = planningFuture.get();  // BLOCKING
}

// Improved (non-blocking):
if (isPlanning && planningFuture != null && planningFuture.isDone()) {
    planningFuture.thenAccept(response -> {
        // Handle response
    }).exceptionally(error -> {
        // Handle error
    });
    isPlanning = false;
    planningFuture = null;
}
```

### Priority 2: Implement Manual Circuit Breaker

**Reason:** Resilience4j circuit breaker is unused due to classloading issues.

**Solution:** Implement `SimpleCircuitBreaker` (see section above) and integrate into `AsyncOpenAIClient`.

### Priority 3: Add Timeout Handling

**Current:** Only request-level timeout (30 seconds).

**Add:** Planning-level timeout with fallback.

```java
public CompletableFuture<ResponseParser.ParsedResponse> planTasksAsync(ForemanEntity foreman, String command) {
    CompletableFuture<ResponseParser.ParsedResponse> planning = executeAsyncRequest(...);

    // Add timeout fallback
    CompletableFuture<ResponseParser.ParsedResponse> timeoutFallback =
        CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(30000);  // 30 second timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return generateFallbackResponse(command, "Planning timeout");
        });

    return planning.applyToEither(timeoutFallback, response -> response);
}
```

### Priority 4: Enhanced Error Context

**Add:** Request ID for tracing, detailed error metrics.

```java
public class AsyncOpenAIClient implements AsyncLLMClient {
    private final AtomicLong requestIdGenerator = new AtomicLong(0);

    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        String requestId = String.valueOf(requestIdGenerator.incrementAndGet());
        long startTime = System.currentTimeMillis();

        LOGGER.debug("[{}] Sending request: {}", requestId, prompt.substring(0, 50));

        return sendWithRetry(request, startTime, 0)
            .thenApply(response -> {
                long latency = System.currentTimeMillis() - startTime;
                metrics.recordSuccess(providerId, latency, response.getTokensUsed());
                LOGGER.debug("[{}] Success in {}ms", requestId, latency);
                return response;
            })
            .exceptionally(error -> {
                long latency = System.currentTimeMillis() - startTime;
                metrics.recordFailure(providerId, latency, error);
                LOGGER.error("[{}] Failed after {}ms: {}", requestId, latency, error.getMessage());
                throw new CompletionException(error);
            });
    }
}
```

### Priority 5: Provider Health Monitoring

**Add:** Health check endpoint and metrics.

```java
public class ProviderHealthMonitor {
    private final ConcurrentMap<String, ProviderHealth> healthMap = new ConcurrentHashMap<>();

    public void recordSuccess(String providerId, long latency) {
        healthMap.compute(providerId, (id, health) -> {
            if (health == null) health = new ProviderHealth();
            health.recordSuccess(latency);
            return health;
        });
    }

    public void recordFailure(String providerId, Throwable error) {
        healthMap.compute(providerId, (id, health) -> {
            if (health == null) health = new ProviderHealth();
            health.recordFailure(error);
            return health;
        });
    }

    public boolean isHealthy(String providerId) {
        ProviderHealth health = healthMap.get(providerId);
        return health != null && health.getSuccessRate() > 0.5;
    }

    public ProviderHealth getHealth(String providerId) {
        return healthMap.getOrDefault(providerId, ProviderHealth.UNKNOWN);
    }
}
```

---

## Summary

### Current State

| Component | Error Handling | Resilience Patterns | Status |
|-----------|---------------|---------------------|--------|
| AsyncOpenAIClient | ExceptionallyCompose, custom retry | Retry with backoff | Good |
| AsyncGroqClient | Basic exceptionally | None | Needs improvement |
| AsyncGeminiClient | Basic exceptionally | None | Needs improvement |
| ResilientLLMClient | Exceptionally with fallback | CB, Retry, RL, Bulkhead | Good but unused |
| TaskPlanner | Exceptionally returns null | Batching | Needs improvement |
| ActionExecutor | Try-catch in tick() | None | Needs improvement |

### Key Recommendations

1. **Implement manual circuit breaker** to replace unused Resilience4j circuit breaker
2. **Fix blocking `planningFuture.get()`** in ActionExecutor.tick()
3. **Add retry logic to AsyncGroqClient and AsyncGeminiClient** (currently missing)
4. **Implement proper Result type** instead of returning null on errors
5. **Add timeout fallback** at planning level (not just request level)
6. **Enhance fallback handler** with context awareness and rule-based actions
7. **Add comprehensive metrics** for error rates, latency, and circuit breaker state
8. **Implement chaos testing** to validate error recovery under stress

---

**Document Version:** 1.0
**Last Updated:** 2025-02-27
**Author:** Analysis based on MineWright codebase at `C:\Users\casey\steve`
