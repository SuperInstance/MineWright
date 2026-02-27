# Async Pattern Improvements for MineWright AI

**Date:** 2025-02-27
**Scope:** Comprehensive review and improvement recommendations for async patterns across the LLM client infrastructure

---

## Executive Summary

This document identifies critical issues in the async/LLM infrastructure and provides concrete improvements for:

1. **CompletableFuture usage** - Memory leaks, missing cancellation, blocking patterns
2. **Thread pool management** - Resource leaks, missing shutdown, improper sizing
3. **Cancellation handling** - Orphaned futures, interrupt propagation
4. **Timeout handling** - Missing timeouts, inconsistent values
5. **Resource cleanup** - ExecutorService leaks, HTTP client lifecycle

**Severity:** High - Multiple resource leaks that will cause issues in production

---

## 1. CompletableFuture Usage Issues

### 1.1 Missing Exception Handling in Chaining

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java` (lines 229-262)

**Problem:**
```java
// Current code - incomplete error handling
if (isPlanning && planningFuture != null && planningFuture.isDone()) {
    try {
        ResponseParser.ParsedResponse response = planningFuture.get();
        // ... process response
    } catch (java.util.concurrent.CancellationException e) {
        // Handles cancellation
    } catch (Exception e) {
        // Generic catch - loses specific error information
    }
}
```

**Issues:**
- `planningFuture.get()` is a blocking call that can hang the game thread
- Catches generic `Exception` instead of specific types
- No timeout on the `get()` call
- Doesn't check `ExecutionException` cause properly

**Recommended Fix:**
```java
if (isPlanning && planningFuture != null && planningFuture.isDone()) {
    try {
        // Use get with timeout to prevent indefinite blocking
        ResponseParser.ParsedResponse response = planningFuture.get(
            100, java.util.concurrent.TimeUnit.MILLISECONDS
        );
        // ... process response
    } catch (java.util.concurrent.TimeoutException e) {
        MineWrightMod.LOGGER.warn("Planning result not ready yet, will check next tick");
        // Don't reset state, will check again
        return;
    } catch (java.util.concurrent.CancellationException e) {
        MineWrightMod.LOGGER.info("Planning was cancelled", e);
        sendToGUI(foreman.getSteveName(), "Planning cancelled.");
    } catch (java.util.concurrent.ExecutionException e) {
        // Unwrap the actual cause
        Throwable cause = e.getCause();
        MineWrightMod.LOGGER.error("Planning failed", cause);
        sendToGUI(foreman.getSteveName(), "Planning failed: " +
            (cause.getMessage() != null ? cause.getMessage() : "Unknown error"));
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        MineWrightMod.LOGGER.warn("Interrupted while waiting for planning result");
        sendToGUI(foreman.getSteveName(), "Planning was interrupted.");
    } finally {
        isPlanning = false;
        planningFuture = null;
        pendingCommand = null;
    }
}
```

### 1.2 Orphaned Futures in Batching Client

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\batch\BatchingLLMClient.java` (lines 179-184)

**Problem:**
```java
// Future returned but not tracked for cancellation
underlyingClient.sendAsync(batch.userPrompt, params)
    .thenAccept(response -> handleSuccess(batch, response))
    .exceptionally(error -> {
        handleError(batch, error);
        return null;
    });
```

**Issues:**
- Returned `CompletableFuture` is not stored
- No way to cancel pending batch sends
- Futures continue even if `BatchingLLMClient.stop()` is called

**Recommended Fix:**
```java
private final ConcurrentHashMap<String, CompletableFuture<LLMResponse>> pendingBatches =
    new ConcurrentHashMap<>();

private void sendBatch(PromptBatcher.CompiledBatch batch) {
    LOGGER.debug("Sending batch {} with {} prompts", batch.id, batch.originalPrompts.size());

    Map<String, Object> params = new HashMap<>(batch.params);
    params.put("systemPrompt", batch.systemPrompt);

    CompletableFuture<LLMResponse> future = underlyingClient.sendAsync(batch.userPrompt, params)
        .thenAccept(response -> {
            pendingBatches.remove(batch.id);
            handleSuccess(batch, response);
        })
        .exceptionally(error -> {
            pendingBatches.remove(batch.id);
            handleError(batch, error);
            return null;
        });

    pendingBatches.put(batch.id, future);
}

// Add to stop() method:
public void stop() {
    running = false;

    // Cancel all pending batches
    for (CompletableFuture<?> future : pendingBatches.values()) {
        future.cancel(true);
    }
    pendingBatches.clear();

    heartbeat.stop();
    batcher.stop();

    LOGGER.info("BatchingLLMClient stopped with {} pending cancellations", pendingBatches.size());
}
```

---

## 2. Thread Pool Management Issues

### 2.1 LLMExecutorService Missing Shutdown Hook

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\LLMExecutorService.java`

**Problem:**
The `LLMExecutorService` has a `shutdown()` method but it's never called automatically.

**Issues:**
- Thread pools continue running after mod/game shutdown
- Daemon threads may prevent clean JVM exit
- Resource leak on server restart

**Recommended Fix:**
```java
// Add shutdown hook registration in constructor or static initializer
private LLMExecutorService() {
    LOGGER.info("Initializing LLM executor service with {} threads per provider", THREADS_PER_PROVIDER);

    // ... existing initialization code ...

    // Register shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        LOGGER.info("LLM executor service shutdown hook triggered");
        shutdown();
    }, "llm-executor-shutdown-hook"));

    LOGGER.info("LLM executor service initialized successfully with shutdown hook");
}

// Also make shutdown() more robust
public synchronized void shutdown() {
    if (isShutdown) {
        LOGGER.debug("LLMExecutorService already shut down, ignoring duplicate shutdown call");
        return;
    }

    LOGGER.info("Shutting down LLM executor service...");
    isShutdown = true;

    // Cancel all pending/futures before shutdown
    shutdownAndAwaitTermination("openai", openaiExecutor);
    shutdownAndAwaitTermination("groq", groqExecutor);
    shutdownAndAwaitTermination("gemini", geminiExecutor);

    LOGGER.info("LLM executor service shut down successfully");
}

private void shutdownAndAwaitTermination(String providerId, ExecutorService executor) {
    try {
        LOGGER.debug("Shutting down {} executor...", providerId);

        // Disable new tasks from being submitted
        executor.shutdown();

        // Wait for existing tasks to terminate
        if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            LOGGER.warn("{} executor did not terminate gracefully, forcing shutdown", providerId);

            // Cancel currently executing tasks
            List<Runnable> droppedTasks = executor.shutdownNow();
            LOGGER.warn("{} executor forced shutdown, dropped {} tasks", providerId, droppedTasks.size());

            // Wait again for tasks to respond to cancellation
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.error("{} executor did not terminate after forced shutdown", providerId);
            }
        } else {
            LOGGER.debug("{} executor shut down gracefully", providerId);
        }
    } catch (InterruptedException e) {
        LOGGER.error("Interrupted while shutting down {} executor", providerId, e);
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

### 2.2 Missing Thread Pool in Retry Scheduler

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\AsyncOpenAIClient.java` (lines 68-73)

**Problem:**
```java
private static final java.util.concurrent.ScheduledExecutorService RETRY_SCHEDULER =
    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "openai-retry-scheduler");
        t.setDaemon(true);
        return t;
    });
```

**Issues:**
- Static ScheduledExecutorService is never shut down
- Creates thread leak on each class load (in modded environments)
- Daemon threads may continue after mod unload

**Recommended Fix:**
```java
// Remove static scheduler, make it instance-based
private final java.util.concurrent.ScheduledExecutorService retryScheduler;

public AsyncOpenAIClient(String apiKey, String model, int maxTokens, double temperature) {
    // ... existing validation code ...

    this.retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "openai-retry-scheduler-" + System.identityHashCode(this));
        t.setDaemon(true);
        return t;
    });

    // ... rest of initialization ...
}

// Add close method
public void close() {
    LOGGER.debug("[openai] Shutting down retry scheduler");
    retryScheduler.shutdown();
    try {
        if (!retryScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
            retryScheduler.shutdownNow();
        }
    } catch (InterruptedException e) {
        retryScheduler.shutdownNow();
        Thread.currentThread().interrupt();
    }
}

// Implement AutoCloseable
// public class AsyncOpenAIClient implements AsyncLLMClient, AutoCloseable {
```

### 2.3 HeartbeatScheduler Thread Leak

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\batch\HeartbeatScheduler.java` (lines 77-86)

**Problem:**
```java
this.scheduler = Executors.newSingleThreadScheduledExecutor(
    r -> new Thread(r, "HeartbeatScheduler")
);
this.callbackExecutor = Executors.newCachedThreadPool(
    r -> {
        Thread t = new Thread(r, "Heartbeat-Callback");
        t.setDaemon(true);
        return t;
    }
);
```

**Issues:**
- `CachedThreadPool` creates unbounded threads under load
- No maximum thread count for callback executor
- Threads may not be daemon (only callback threads are)

**Recommended Fix:**
```java
// Use bounded thread pool instead of cached
this.scheduler = Executors.newSingleThreadScheduledExecutor(
    r -> {
        Thread t = new Thread(r, "HeartbeatScheduler");
        t.setDaemon(true);
        return t;
    }
);

// Bounded thread pool with queue
this.callbackExecutor = new ThreadPoolExecutor(
    1, // corePoolSize
    5, // maximumPoolSize
    60L, TimeUnit.SECONDS, // keepAliveTime
    new LinkedBlockingQueue<>(100), // bounded queue
    r -> {
        Thread t = new Thread(r, "Heartbeat-Callback");
        t.setDaemon(true);
        return t;
    },
    new ThreadPoolExecutor.CallerRunsPolicy() // fallback policy
);

// Improve shutdown
public synchronized void stop() {
    running = false;

    if (heartbeatTask != null) {
        heartbeatTask.cancel(false);
    }

    // Shutdown scheduler first
    scheduler.shutdown();
    callbackExecutor.shutdown();

    try {
        // Wait for scheduler
        if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
        }

        // Wait for callback executor
        if (!callbackExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
            List<Runnable> dropped = callbackExecutor.shutdownNow();
            LOGGER.warn("HeartbeatScheduler stopped with {} dropped callbacks", dropped.size());
            callbackExecutor.awaitTermination(2, TimeUnit.SECONDS);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    LOGGER.info("HeartbeatScheduler stopped");
}
```

---

## 3. Cancellation Handling Issues

### 3.1 No Cancellation of Pending Plans

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java` (lines 117-162)

**Problem:**
```java
public void processNaturalLanguageCommand(String command) {
    // If already planning, ignores new command
    if (isPlanning) {
        MineWrightMod.LOGGER.warn("Foreman '{}' is already planning, ignoring command: {}",
            foreman.getSteveName(), command);
        sendToGUI(foreman.getSteveName(),
            "Hold on, I'm still thinking about the previous command...");
        return;
    }

    // No cancellation of existing planningFuture
    // If isPlanning is true, future is orphaned
}
```

**Issues:**
- New commands are rejected if planning is in progress
- No way to cancel the current planning operation
- User must wait for LLM timeout before trying again

**Recommended Fix:**
```java
public void processNaturalLanguageCommand(String command) {
    MineWrightMod.LOGGER.info("Foreman '{}' processing command (async): {}",
        foreman.getSteveName(), command);

    // Cancel existing planning if any
    if (isPlanning && planningFuture != null) {
        MineWrightMod.LOGGER.info("Foreman '{}' cancelling previous planning for new command",
            foreman.getSteveName());

        boolean cancelled = planningFuture.cancel(true);
        if (cancelled) {
            sendToGUI(foreman.getSteveName(), "Previous command cancelled, starting new one...");
        } else {
            // Too late to cancel, already processing
            sendToGUI(foreman.getSteveName(),
                "Hold on, I'm almost done with the previous command...");
            return;
        }
    }

    // Cancel current actions
    if (currentAction != null) {
        currentAction.cancel();
        currentAction = null;
    }

    if (idleFollowAction != null) {
        idleFollowAction.cancel();
        idleFollowAction = null;
    }

    try {
        this.pendingCommand = command;
        this.isPlanning = true;

        sendToGUI(foreman.getSteveName(), "Thinking...");

        // Store future reference for potential cancellation
        planningFuture = getTaskPlanner().planTasksAsync(foreman, command)
            .orTimeout(60, TimeUnit.SECONDS) // Add timeout
            .exceptionally(throwable -> {
                if (throwable instanceof TimeoutException) {
                    MineWrightMod.LOGGER.error("Planning timed out for: {}", command);
                    sendToGUI(foreman.getSteveName(), "Sorry, that's taking too long...");
                }
                throw new CompletionException(throwable);
            });

        MineWrightMod.LOGGER.info("Foreman '{}' started async planning for: {}",
            foreman.getSteveName(), command);

    } catch (NoClassDefFoundError e) {
        MineWrightMod.LOGGER.error("Failed to initialize AI components", e);
        sendToGUI(foreman.getSteveName(), "Sorry, I'm having trouble with my AI systems!");
        isPlanning = false;
        planningFuture = null;
    } catch (Exception e) {
        MineWrightMod.LOGGER.error("Error starting async planning", e);
        sendToGUI(foreman.getSteveName(), "Oops, something went wrong!");
        isPlanning = false;
        planningFuture = null;
    }
}

// Add to stopCurrentAction()
public void stopCurrentAction() {
    // Cancel pending planning
    if (isPlanning && planningFuture != null) {
        planningFuture.cancel(true);
        isPlanning = false;
        planningFuture = null;
        pendingCommand = null;
    }

    if (currentAction != null) {
        currentAction.cancel();
        currentAction = null;
    }
    if (idleFollowAction != null) {
        idleFollowAction.cancel();
        idleFollowAction = null;
    }
    taskQueue.clear();
    currentGoal = null;

    // Reset state machine
    stateMachine.reset();
}
```

### 3.2 CompletableFuture in ActionExecutor Not Interruptible

**Problem:**
The `planningFuture.get()` call in `tick()` can block indefinitely if the future never completes.

**Recommended Fix:**
Already covered in section 1.1 - use `get(timeout)` instead of `get()`.

---

## 4. Timeout Handling Issues

### 4.1 Inconsistent Timeouts Across Clients

**Location:** Multiple async client files

**Current State:**
| Client | HTTP Timeout | Retry Timeout | Total Max |
|--------|-------------|---------------|-----------|
| AsyncOpenAIClient | 30s | 7s (1s+2s+4s) | ~51s |
| AsyncGroqClient | 30s | None | 30s |
| AsyncGeminiClient | 60s | None | 60s |

**Issues:**
- Groq and Gemini have no retry logic at all
- Gemini timeout is double OpenAI/Groq
- No application-level timeout to prevent infinite hangs

**Recommended Fix:**

Create a centralized timeout configuration:

```java
// New file: src/main/java/com/minewright/llm/async/TimeoutConfig.java
package com.minewright.llm.async;

import java.time.Duration;

public final class TimeoutConfig {
    private TimeoutConfig() {}

    // HTTP client timeouts
    public static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration HTTP_REQUEST_TIMEOUT_FAST = Duration.ofSeconds(30);
    public static final Duration HTTP_REQUEST_TIMEOUT_SLOW = Duration.ofSeconds(60);

    // Retry configuration
    public static final int MAX_RETRIES = 3;
    public static final long INITIAL_BACKOFF_MS = 1000;
    public static final double BACKOFF_MULTIPLIER = 2.0;

    // Application-level timeout (absolute max)
    public static final Duration ABSOLUTE_TIMEOUT = Duration.ofSeconds(90);

    // Provider-specific overrides
    public static Duration getHttpTimeoutForProvider(String providerId) {
        return switch (providerId.toLowerCase()) {
            case "gemini" -> HTTP_REQUEST_TIMEOUT_SLOW;
            default -> HTTP_REQUEST_TIMEOUT_FAST;
        };
    }

    public static boolean shouldRetry(String providerId) {
        // All providers should retry
        return true;
    }
}
```

Update clients to use this configuration:

```java
// In AsyncOpenAIClient
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(OPENAI_API_URL))
    .header("Content-Type", "application/json")
    .header("Authorization", "Bearer " + apiKey)
    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
    .timeout(TimeoutConfig.getHttpTimeoutForProvider(PROVIDER_ID))
    .build();

// In TaskPlanner, add application-level timeout:
public CompletableFuture<ResponseParser.ParsedResponse> planTasksAsync(
        ForemanEntity foreman, String command, boolean isUserInitiated) {
    // ... existing setup code ...

    CompletableFuture<ResponseParser.ParsedResponse> result = executeAsyncRequest(client, userPrompt, params);

    // Add application-level timeout
    return result.orTimeout(
        TimeoutConfig.ABSOLUTE_TIMEOUT.toSeconds(),
        TimeUnit.SECONDS
    ).exceptionally(throwable -> {
        if (throwable instanceof TimeoutException) {
            MineWrightMod.LOGGER.error("[Async] Request exceeded absolute timeout of {}",
                TimeoutConfig.ABSOLUTE_TIMEOUT);
        }
        throw new CompletionException(throwable);
    });
}
```

### 4.2 Missing Timeout in Batching Client

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\batch\PromptBatcher.java`

**Problem:**
Batches can wait indefinitely in `waitForMinInterval()` (line 404-419).

**Recommended Fix:**
```java
private void waitForMinInterval() {
    if (lastSendTime == Instant.MIN) {
        return;
    }

    long adjustedInterval = (long) (MIN_BATCH_INTERVAL_MS * backoffMultiplier);
    long elapsed = Duration.between(lastSendTime, Instant.now()).toMillis();
    long remaining = adjustedInterval - elapsed;

    // Cap wait time to prevent blocking too long
    long maxWait = 5000; // 5 seconds max wait
    if (remaining > 0) {
        long actualWait = Math.min(remaining, maxWait);
        try {
            Thread.sleep(actualWait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while waiting for min interval");
        }
    }
}
```

---

## 5. Resource Cleanup Issues

### 5.1 TaskPlanner Missing Lifecycle Management

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\TaskPlanner.java`

**Problem:**
`TaskPlanner` has a `shutdown()` method but it's not called from entity cleanup.

**Issues:**
- `BatchingLLMClient` may continue running after entity removal
- Thread pools not cleaned up
- Memory leak from cached responses

**Recommended Fix:**
```java
// In ForemanEntity, add cleanup hook:
@Override
public void remove(RemovalReason reason) {
    LOGGER.info("Removing Foreman entity '{}', cleaning up resources", getSteveName());

    // Shutdown task planner and its resources
    if (actionExecutor != null) {
        actionExecutor.stopCurrentAction();
        // TaskPlanner should be shut down when entity is removed
        if (actionExecutor.getTaskPlanner() != null) {
            actionExecutor.getTaskPlanner().shutdown();
        }
    }

    super.remove(reason);
}

// Improve TaskPlanner.shutdown():
public void shutdown() {
    LOGGER.info("Shutting down TaskPlanner");

    // Stop batching client first
    if (batchingClient != null) {
        batchingClient.stop();
        batchingClient = null;
    }

    // Close async clients if they implement AutoCloseable
    shutdownAsyncClient(asyncOpenAIClient);
    shutdownAsyncClient(asyncGroqClient);
    shutdownAsyncClient(asyncGeminiClient);

    // Clear cache
    if (llmCache != null) {
        llmCache.clear();
    }

    LOGGER.info("TaskPlanner shutdown complete");
}

private void shutdownAsyncClient(AsyncLLMClient client) {
    if (client instanceof AutoCloseable) {
        try {
            ((AutoCloseable) client).close();
        } catch (Exception e) {
            LOGGER.warn("Error closing async client: {}", e.getMessage());
        }
    }
}
```

### 5.2 HttpClient Not Closed

**Problem:**
All async clients create `HttpClient` instances but never close them.

**Issues:**
- While `HttpClient` doesn't strictly require closing (it's shared internally), it's good practice
- Resources (selector threads, connections) may not be released promptly

**Recommended Fix:**
```java
public class AsyncOpenAIClient implements AsyncLLMClient, AutoCloseable {

    private final HttpClient httpClient;
    // ... other fields ...

    @Override
    public void close() {
        // Close retry scheduler
        if (retryScheduler != null && !retryScheduler.isShutdown()) {
            retryScheduler.shutdown();
            try {
                if (!retryScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    retryScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                retryScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // HttpClient doesn't need explicit closing, but we can log
        LOGGER.debug("[openai] HttpClient resources released");
    }
}
```

---

## 6. Additional Improvements

### 6.1 Add Circuit Breaker to All Clients

**Current State:**
Only `ResilientLLMClient` has circuit breaker, but direct async clients don't.

**Recommendation:**
Either:
1. Always use `ResilientLLMClient` wrapper
2. Add circuit breaker directly to each client

### 6.2 Add Metrics/Monitoring

**Recommendation:**
```java
// Add to each async client
private final AtomicLong successCount = new AtomicLong(0);
private final AtomicLong failureCount = new AtomicLong(0);
private final AtomicLong totalLatencyMs = new AtomicLong(0);

public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
    long startTime = System.currentTimeMillis();

    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
            long latency = System.currentTimeMillis() - startTime;
            successCount.incrementAndGet();
            totalLatencyMs.addAndGet(latency);
            // ... existing code ...
        })
        .exceptionally(error -> {
            failureCount.incrementAndGet();
            // ... existing error handling ...
        });
}

public ClientStats getStats() {
    long successes = successCount.get();
    long failures = failureCount.get();
    long total = successes + failures;
    double avgLatency = successes > 0 ?
        (double) totalLatencyMs.get() / successes : 0;

    return new ClientStats(successes, failures, total, avgLatency);
}

public record ClientStats(long successes, long failures, long total, double avgLatencyMs) {}
```

### 6.3 Add Request Context for Tracing

**Recommendation:**
```java
public class RequestContext {
    private final String requestId;
    private final Instant createdAt;
    private final String foremanName;
    private final String command;

    public RequestContext(String foremanName, String command) {
        this.requestId = UUID.randomUUID().toString().substring(0, 8);
        this.createdAt = Instant.now();
        this.foremanName = foremanName;
        this.command = command;
    }

    // Include in all log statements for traceability
    public String toLogPrefix() {
        return String.format("[%s][%s]", requestId, foremanName);
    }
}
```

---

## 7. Implementation Priority

### High Priority (Fix Soon)
1. **Fix LLMExecutorService shutdown** - Add shutdown hook (2.1)
2. **Fix ActionExecutor blocking get()** - Use timeout (1.1)
3. **Fix TaskPlanner shutdown** - Call from entity removal (5.1)
4. **Fix AsyncOpenAIClient retry scheduler** - Make instance-based (2.2)

### Medium Priority (Fix Next)
5. **Add cancellation to processNaturalLanguageCommand** - Allow cancel (3.1)
6. **Fix HeartbeatScheduler thread pool** - Bounded pool (2.3)
7. **Add centralized timeout config** - Consistent timeouts (4.1)
8. **Track pending batches for cancellation** - In BatchingLLMClient (1.2)

### Low Priority (Nice to Have)
9. **Add metrics/monitoring** - For observability (6.2)
10. **Add request tracing** - For debugging (6.3)
11. **Implement AutoCloseable** - On all clients (5.2)

---

## 8. Code Changes Summary

| File | Change | Lines | Priority |
|------|--------|-------|----------|
| `LLMExecutorService.java` | Add shutdown hook, improve shutdown | ~30 | High |
| `ActionExecutor.java` | Fix blocking get(), add cancellation | ~20 | High |
| `TaskPlanner.java` | Improve shutdown | ~15 | High |
| `AsyncOpenAIClient.java` | Fix retry scheduler lifecycle | ~25 | High |
| `HeartbeatScheduler.java` | Fix thread pool sizing | ~30 | Medium |
| `BatchingLLMClient.java` | Track pending futures | ~20 | Medium |
| `TimeoutConfig.java` | NEW FILE | ~40 | Medium |
| `AsyncGroqClient.java` | Add retry logic | ~50 | Medium |
| `AsyncGeminiClient.java` | Add retry logic | ~50 | Medium |

**Total estimated changes:** ~280 lines across 9 files

---

## 9. Testing Recommendations

After implementing these fixes, test:

1. **Thread leak test:** Start/stop server 10 times, check thread count
2. **Cancellation test:** Send command, immediately send another
3. **Timeout test:** Mock slow LLM responses, verify timeout handling
4. **Memory leak test:** Run for 1 hour with continuous commands
5. **Concurrent load test:** 10 agents planning simultaneously

---

## 10. References

- **Files Analyzed:**
  - `src/main/java/com/minewright/llm/async/LLMExecutorService.java`
  - `src/main/java/com/minewright/llm/async/AsyncLLMClient.java`
  - `src/main/java/com/minewright/llm/async/AsyncOpenAIClient.java`
  - `src/main/java/com/minewright/llm/async/AsyncGroqClient.java`
  - `src/main/java/com/minewright/llm/async/AsyncGeminiClient.java`
  - `src/main/java/com/minewright/llm/async/LLMException.java`
  - `src/main/java/com/minewright/llm/async/LLMCache.java`
  - `src/main/java/com/minewright/llm/batch/BatchingLLMClient.java`
  - `src/main/java/com/minewright/llm/batch/HeartbeatScheduler.java`
  - `src/main/java/com/minewright/llm/batch/PromptBatcher.java`
  - `src/main/java/com/minewright/llm/TaskPlanner.java`
  - `src/main/java/com/minewright/action/ActionExecutor.java`
  - `src/main/java/com/minewright/llm/resilience/ResilientLLMClient.java`
  - `src/main/java/com/minewright/llm/resilience/ResilienceConfig.java`

- **Java Documentation:**
  - [CompletableFuture](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletableFuture.html)
  - [ExecutorService](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ExecutorService.html)
  - [HttpClient](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html)
