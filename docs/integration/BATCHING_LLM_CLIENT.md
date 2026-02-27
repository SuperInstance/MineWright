# BatchingLLMClient Implementation Plan

## Executive Summary

This document details the design and implementation of an enhanced `BatchingLLMClient` for MineWright. The system implements intelligent request batching to reduce API calls, respect rate limits, and improve overall efficiency while maintaining low latency for user interactions.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Class Design](#class-design)
4. [Data Structures](#data-structures)
5. [Threading Model](#threading-model)
6. [Error Handling](#error-handling)
7. [Configuration](#configuration)
8. [Metrics](#metrics)
9. [Integration](#integration)
10. [Implementation Phases](#implementation-phases)

---

## Overview

### Goals

1. **Reduce API Calls**: Batch multiple LLM requests into single API calls
2. **Respect Rate Limits**: Avoid 429 errors through intelligent timing
3. **Maintain Responsiveness**: Keep user-facing requests fast
4. **Graceful Degradation**: Handle failures without losing requests
5. **Observable Metrics**: Track batch efficiency and performance

### Key Features

- **Time-windowed batching**: Configurable wait time (default 100ms) for accumulating requests
- **Size-limited batches**: Maximum batch size (default 10) to prevent oversized requests
- **Priority queuing**: User interactions get priority over background tasks
- **Per-request callbacks**: Each request gets its own CompletableFuture
- **Thread-safe**: Uses Java concurrent utilities throughout
- **Graceful shutdown**: Completes pending requests on shutdown
- **Batch efficiency metrics**: Track batching performance

---

## Architecture

### Current System Analysis

The existing `BatchingLLMClient` (in `com.minewright.llm.batch`) provides:
- Basic batching with `PromptBatcher`
- Heartbeat-based scheduling with `HeartbeatScheduler`
- Local prompt preprocessing with `LocalPreprocessor`
- Priority-based queuing

### Enhanced Architecture

```
                           User Request
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       BatchingLLMClient                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │   Request    │───►│   Priority   │───►│   Batch Window   │  │
│  │   Validator  │    │    Queue     │    │   Accumulator    │  │
│  └──────────────┘    └──────────────┘    └──────────────────┘  │
│                                  │                  │             │
│                                  │                  ▼             │
│                                  │         ┌───────────────┐     │
│                                  │         │   Batch       │     │
│                                  │         │   Compiler    │     │
│                                  │         └───────────────┘     │
│                                  │                  │             │
│                                  ▼                  ▼             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Batch Dispatcher                       │   │
│  │  - Checks batch size limits                              │   │
│  │  - Checks time window                                    │   │
│  │  - Forces batch on timeout                               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                  │                               │
│                                  ▼                               │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  Response Router                          │   │
│  │  - Parses batched responses                              │   │
│  │  - Routes to individual callbacks                        │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
                     ┌──────────────────┐
                     │  AsyncLLMClient  │
                     │  (underlying)    │
                     └──────────────────┘
```

---

## Class Design

### Main Class: BatchingLLMClient

**Package**: `com.minewright.llm.batch`

**Purpose**: Main entry point for batched LLM requests

```java
public class BatchingLLMClient {

    // === Configuration ===
    private final BatchingConfig config;

    // === Core Components ===
    private final AsyncLLMClient underlyingClient;
    private final RequestQueue requestQueue;
    private final BatchDispatcher dispatcher;
    private final ResponseRouter responseRouter;
    private final BatchMetrics metrics;

    // === State ===
    private final AtomicBoolean running;
    private final ScheduledExecutorService scheduler;

    /**
     * Creates a new BatchingLLMClient.
     *
     * @param underlyingClient The underlying LLM client
     * @param config Batching configuration (null for defaults)
     */
    public BatchingLLMClient(AsyncLLMClient underlyingClient,
                             BatchingConfig config);

    /**
     * Starts the batching system.
     */
    public void start();

    /**
     * Stops the batching system with graceful shutdown.
     * Completes all pending requests before returning.
     *
     * @param timeoutMs Maximum time to wait for pending requests
     */
    public void stop(long timeoutMs);

    // === Request Submission ===

    /**
     * Submits a request for batching.
     *
     * @param request The LLM request
     * @return CompletableFuture that will complete with the response
     */
    public CompletableFuture<LLMResponse> submit(BatchingRequest request);

    /**
     * Submits a high-priority user interaction request.
     * These bypass normal batching delays.
     *
     * @param prompt The prompt text
     * @param params Request parameters
     * @return CompletableFuture with the response
     */
    public CompletableFuture<LLMResponse> submitUrgent(String prompt,
                                                       Map<String, Object> params);

    /**
     * Submits a background task request.
     * These are batched aggressively.
     *
     * @param prompt The prompt text
     * @param params Request parameters
     * @return CompletableFuture with the response
     */
    public CompletableFuture<LLMResponse> submitBackground(String prompt,
                                                          Map<String, Object> params);

    // === Metrics and Monitoring ===

    /**
     * Gets the current batch efficiency metrics.
     */
    public BatchMetrics getMetrics();

    /**
     * Gets the current queue size.
     */
    public int getQueueSize();

    /**
     * Gets the current status summary.
     */
    public String getStatusSummary();
}
```

### Configuration Class: BatchingConfig

```java
public class BatchingConfig {

    // Time window (ms) to wait for batch accumulation
    private final long batchWindowMs;

    // Maximum requests per batch
    private final int maxBatchSize;

    // Minimum requests to trigger early batch send
    private final int minBatchSize;

    // Maximum time to wait before forcing batch send
    private final long maxWaitTimeMs;

    // Priority queue capacity
    private final int queueCapacity;

    // Number of dispatcher threads
    private final int dispatcherThreads;

    // Enable request deduplication
    private final boolean enableDeduplication;

    // Enable local preprocessing
    private final boolean enablePreprocessing;

    /**
     * Creates a builder for configuration.
     */
    public static Builder builder();

    /**
     * Creates default configuration.
     */
    public static BatchingConfig defaults();

    // Default values
    public static final long DEFAULT_BATCH_WINDOW_MS = 100;
    public static final int DEFAULT_MAX_BATCH_SIZE = 10;
    public static final int DEFAULT_MIN_BATCH_SIZE = 2;
    public static final long DEFAULT_MAX_WAIT_TIME_MS = 5000;
    public static final int DEFAULT_QUEUE_CAPACITY = 1000;
    public static final int DEFAULT_DISPATCHER_THREADS = 2;
}
```

### Request Wrapper: BatchingRequest

```java
public class BatchingRequest {

    // Unique request identifier
    private final String requestId;

    // The prompt text
    private final String prompt;

    // Request parameters
    private final Map<String, Object> params;

    // Priority level
    private final RequestPriority priority;

    // Callback for response
    private final CompletableFuture<LLMResponse> responseFuture;

    // Timestamp when request was created
    private final Instant createdAt;

    // Maximum wait time before forcing send
    private final Duration maxWaitTime;

    // Additional metadata
    private final Map<String, Object> metadata;

    /**
     * Request priority levels.
     */
    public enum RequestPriority {
        URGENT(100),      // User interactions - process immediately
        HIGH(75),         // Important tasks
        NORMAL(50),       // Standard requests
        LOW(25),          // Background tasks
        DEFERRABLE(10);   // Can wait indefinitely

        private final int value;
        RequestPriority(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    /**
     * Creates a new request.
     */
    public static BatchingRequest create(String prompt,
                                        Map<String, Object> params,
                                        RequestPriority priority);
}
```

---

## Data Structures

### Request Queue

**Implementation**: `PriorityBlockingQueue<BatchingRequest>`

**Purpose**: Thread-safe priority queue for pending requests

```java
public class RequestQueue {

    private final PriorityBlockingQueue<BatchingRequest> queue;
    private final AtomicInteger queueSize;
    private final Semaphore capacitySemaphore;

    /**
     * Adds a request to the queue.
     * Blocks if queue is at capacity.
     *
     * @return true if added successfully
     */
    public boolean offer(BatchingRequest request);

    /**
     * Removes and returns the highest priority request.
     *
     * @return The request, or null if empty
     */
    public BatchingRequest poll();

    /**
     * Drains requests into a list, up to maxBatchSize.
     *
     * @param maxSize Maximum number of requests to drain
     * @return List of drained requests
     */
    public List<BatchingRequest> drain(int maxSize);

    /**
     * Gets current queue size.
     */
    public int size();

    /**
     * Checks if queue is empty.
     */
    public boolean isEmpty();
}
```

### Batch Container

**Purpose**: Holds a collection of requests being batched together

```java
public class BatchContainer {

    // Unique batch ID
    private final String batchId;

    // Requests in this batch
    private final List<BatchingRequest> requests;

    // Compiled system prompt
    private final String systemPrompt;

    // Compiled user prompt
    private final String userPrompt;

    // Batch parameters
    private final Map<String, Object> params;

    // When batch was created
    private final Instant createdAt;

    // When batch should be forced to send
    private final Instant forceSendAt;

    /**
     * Creates a new batch container.
     */
    public BatchContainer(List<BatchingRequest> requests);

    /**
     * Gets the total estimated tokens for this batch.
     */
    public int estimateTokens();

    /**
     * Gets the age of the batch in milliseconds.
     */
    public long getAgeMs();

    /**
     * Checks if batch should be force-sent.
     */
    public boolean shouldForceSend();

    /**
     * Completes all requests with the given response.
     */
    public void completeAll(String response);

    /**
     * Completes all requests with an exception.
     */
    public void completeAllExceptionally(Throwable error);

    /**
     * Completes individual requests from a parsed batch response.
     */
    public void completeIndividual(Map<Integer, String> responses);
}
```

### Batch Accumulator State Machine

**Purpose**: Tracks the state of batch accumulation

```java
public enum BatchState {
    /** Accumulating requests within time window */
    ACCUMULATING,

    /** Batch is full and ready to send */
    READY,

    /** Batch is being sent to LLM */
    SENDING,

    /** Waiting for response */
    AWAITING_RESPONSE,

    /** Response received, processing */
    PROCESSING_RESPONSE,

    /** Batch completed successfully */
    COMPLETED,

    /** Batch failed */
    FAILED
}

public class BatchAccumulator {

    private volatile BatchState state;
    private final List<BatchingRequest> currentBatch;
    private final Instant windowStart;
    private final Lock lock;
    private final Condition condition;

    /**
     * Adds a request to the current batch.
     *
     * @return true if added, false if batch is full/closed
     */
    public boolean addRequest(BatchingRequest request);

    /**
     * Checks if batch is ready to send.
     * Batch is ready if:
     * - Max size reached
     * - Time window expired
     * - Any request exceeded max wait time
     *
     * @return true if ready to send
     */
    public boolean isReadyToSend();

    /**
     * Closes the batch and prepares for sending.
     *
     * @return The compiled batch
     */
    public BatchContainer closeBatch();

    /**
     * Resets for next batch.
     */
    public void reset();
}
```

---

## Threading Model

### Thread Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Thread Pool Architecture                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────┐    ┌──────────────────────────────────┐    │
│  │  Request Queue │    │   Dispatcher Thread Pool          │    │
│  │  (Thread-Safe) │───►│   - Checks batch readiness        │    │
│  └────────────────┘    │   - Compiles batches              │    │
│                        │   - Sends to LLM                   │    │
│                        └──────────────────────────────────┘    │
│                                      │                          │
│                                      ▼                          │
│                        ┌──────────────────────────────────┐    │
│                        │   LLM HTTP Thread                │    │
│                        │   (from underlying client)        │    │
│                        └──────────────────────────────────┘    │
│                                      │                          │
│                                      ▼                          │
│                        ┌──────────────────────────────────┐    │
│                        │   Response Router Thread         │    │
│                        │   - Parses responses             │    │
│                        │   - Completes futures            │    │
│                        └──────────────────────────────────┘    │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │   Scheduled Thread Pool (Maintenance)                  │    │
│  │   - Monitors batch timeouts                            │    │
│  │   - Forces sends on max wait time                      │    │
│  │   - Collects metrics                                   │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Thread Pool Configuration

```java
public class ThreadPoolConfig {

    /**
     * Dispatcher thread pool.
     * Handles batch compilation and sending.
     */
    public static final int DISPATCHER_CORE_THREADS = 2;
    public static final int DISPATCHER_MAX_THREADS = 4;
    public static final int DISPATCHER_QUEUE_SIZE = 100;
    public static final long DISPATCHER_KEEPALIVE_SEC = 60;

    /**
     * Response router thread pool.
     * Handles response parsing and callback execution.
     */
    public static final int ROUTER_CORE_THREADS = 1;
    public static final int ROUTER_MAX_THREADS = 2;

    /**
     * Scheduled executor for maintenance.
     * Handles timeouts and metrics.
     */
    public static final int SCHEDULER_THREADS = 1;

    /**
     * Creates the dispatcher thread pool.
     */
    public static ThreadPoolExecutor createDispatcherPool();

    /**
     * Creates the router thread pool.
     */
    public static ExecutorService createRouterPool();

    /**
     * Creates the scheduled executor.
     */
    public static ScheduledExecutorService createScheduler();
}
```

### Concurrency Utilities Used

1. **Blocking Collections**:
   - `PriorityBlockingQueue<BatchingRequest>` - Request queue
   - `LinkedBlockingQueue<BatchContainer>` - Batch send queue

2. **Synchronization**:
   - `ReentrantLock` - Batch accumulator lock
   - `Condition` - Wait for batch readiness
   - `Semaphore` - Queue capacity control

3. **Atomic Variables**:
   - `AtomicBoolean` - Running state
   - `AtomicInteger` - Queue size, batch counters
   - `AtomicLong` - Metrics counters

4. **Thread Pools**:
   - `ThreadPoolExecutor` - Dispatchers and routers
   - `ScheduledExecutorService` - Maintenance tasks

---

## Error Handling

### Error Handling Strategy

```java
public class BatchErrorHandler {

    /**
     * Handles errors during batch processing.
     *
     * Strategy:
     * 1. Check if error is retryable (rate limit, timeout, network)
     * 2. If retryable and retries remaining, requeue batch
     * 3. If non-retryable or retries exhausted, complete individual requests
     * 4. Log error with context
     * 5. Update metrics
     *
     * @param batch The failed batch
     * @param error The error that occurred
     * @param retryCount Current retry attempt
     */
    public void handleBatchError(BatchContainer batch,
                                Throwable error,
                                int retryCount);

    /**
     * Checks if an error is retryable.
     */
    public boolean isRetryable(Throwable error);

    /**
     * Extracts error type from throwable.
     */
    public ErrorType classifyError(Throwable error);

    public enum ErrorType {
        RATE_LIMIT,          // Retry with exponential backoff
        TIMEOUT,             // Retry once
        NETWORK_ERROR,       // Retry with backoff
        SERVER_ERROR,        // Retry with backoff
        AUTH_ERROR,          // Non-retryable
        INVALID_RESPONSE,    // Non-retryable
        CLIENT_ERROR,        // Non-retryable
        UNKNOWN              // Retry once
    }
}
```

### Retry Logic

```java
public class BatchRetryPolicy {

    private final int maxRetries;
    private final long initialBackoffMs;
    private final double backoffMultiplier;
    private final long maxBackoffMs;

    /**
     * Calculates backoff delay for given retry attempt.
     *
     * Formula: min(initial * multiplier^attempt, max)
     */
    public long calculateBackoff(int attempt);

    /**
     * Default retry policy.
     * - Max retries: 3
     * - Initial backoff: 1000ms
     * - Multiplier: 2.0
     * - Max backoff: 10000ms
     */
    public static BatchRetryPolicy defaults();
}
```

### Circuit Breaker Integration

The batching client integrates with the existing `ResilientLLMClient`:

```java
// When sending batches, check circuit breaker state first
if (!underlyingClient.isHealthy()) {
    // Circuit is OPEN - use fallback strategy
    return handleCircuitOpen(batch);
}

// Send batch
CompletableFuture<LLMResponse> future = underlyingClient.sendAsync(...);
```

---

## Configuration

### Configuration File Integration

Add to `MineWrightConfig.java`:

```java
public class MineWrightConfig {
    // ... existing config ...

    // Batching configuration
    public static final ForgeConfigSpec.BooleanValue ENABLE_BATCHING;
    public static final ForgeConfigSpec.IntValue BATCH_WINDOW_MS;
    public static final ForgeConfigSpec.IntValue MAX_BATCH_SIZE;
    public static final ForgeConfigSpec.IntValue MIN_BATCH_SIZE;
    public static final ForgeConfigSpec.IntValue MAX_WAIT_TIME_MS;
    public static final ForgeConfigSpec.IntValue QUEUE_CAPACITY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DEDUPLICATION;

    static {
        // ... existing config ...

        builder.comment("LLM Batching Configuration").push("batching");

        ENABLE_BATCHING = builder
            .comment("Enable request batching to reduce API calls")
            .define("enabled", true);

        BATCH_WINDOW_MS = builder
            .comment("Time window (ms) to accumulate requests before sending")
            .defineInRange("batchWindowMs", 100, 10, 5000);

        MAX_BATCH_SIZE = builder
            .comment("Maximum requests per batch")
            .defineInRange("maxBatchSize", 10, 1, 50);

        MIN_BATCH_SIZE = builder
            .comment("Minimum requests to trigger early batch send")
            .defineInRange("minBatchSize", 2, 1, 20);

        MAX_WAIT_TIME_MS = builder
            .comment("Maximum time (ms) to wait before forcing batch send")
            .defineInRange("maxWaitTimeMs", 5000, 500, 30000);

        QUEUE_CAPACITY = builder
            .comment("Maximum pending requests in queue")
            .defineInRange("queueCapacity", 1000, 100, 10000);

        ENABLE_DEDUPLICATION = builder
            .comment("Enable automatic deduplication of identical requests")
            .define("enableDeduplication", true);

        builder.pop();
    }
}
```

### Programmatic Configuration

```java
// Create custom configuration
BatchingConfig config = BatchingConfig.builder()
    .batchWindowMs(150)
    .maxBatchSize(15)
    .minBatchSize(3)
    .maxWaitTimeMs(3000)
    .queueCapacity(500)
    .enableDeduplication(true)
    .enablePreprocessing(true)
    .build();

// Or use defaults
BatchingConfig config = BatchingConfig.defaults();

// Create batching client
BatchingLLMClient client = new BatchingLLMClient(underlyingClient, config);
client.start();
```

---

## Metrics

### Metrics Collection

```java
public class BatchMetrics {

    // === Request Metrics ===
    private final AtomicLong totalRequests;
    private final AtomicLong urgentRequests;
    private final AtomicLong backgroundRequests;

    // === Batch Metrics ===
    private final AtomicLong totalBatches;
    private final AtomicLong totalRequestsBatched;
    private final AtomicLong averageBatchSize;

    // === Timing Metrics ===
    private final AtomicLong totalQueueTimeMs;
    private final AtomicLong totalBatchTimeMs;
    private final AtomicLong totalResponseTimeMs;

    // === Efficiency Metrics ===
    private final AtomicDouble batchingEfficiency; // requests/batches
    private final AtomicDouble queueUtilization;    // current/max size
    private final AtomicInteger currentQueueSize;

    // === Error Metrics ===
    private final AtomicLong failedBatches;
    private final AtomicLong retriedBatches;
    private final AtomicLong rateLimitErrors;

    // === Cache Metrics ===
    private final AtomicLong deduplicatedRequests;

    /**
     * Records a submitted request.
     */
    public void recordRequest(BatchingRequest request);

    /**
     * Records a sent batch.
     */
    public void recordBatch(BatchContainer batch);

    /**
     * Records a successful response.
     */
    public void recordResponse(BatchContainer batch, long duration);

    /**
     * Records an error.
     */
    public void recordError(Throwable error);

    /**
     * Gets batch efficiency score (0-1).
     * Higher is better.
     *
     * Efficiency = (actualBatchSize / maxBatchSize) * (requestsBatches / totalRequests)
     */
    public double getEfficiencyScore();

    /**
     * Gets average latency in milliseconds.
     */
    public double getAverageLatency();

    /**
     * Gets metrics summary as JSON.
     */
    public String toJson();

    /**
     * Resets all metrics.
     */
    public void reset();
}
```

### Metrics Reporting

```java
public interface MetricsReporter {

    /**
     * Reports metrics to a monitoring system.
     */
    void report(BatchMetrics metrics);

    /**
     * Console reporter for debugging.
     */
    static MetricsReporter console() {
        return metrics -> System.out.println(metrics.toJson());
    }

    /**
     * SLF4J reporter.
     */
    static MetricsReporter slf4j(Logger logger) {
        return metrics -> logger.info("Batch metrics: {}", metrics.getSummary());
    }
}
```

---

## Integration

### Integration with AsyncLLMClient

The `BatchingLLMClient` wraps any `AsyncLLMClient` implementation:

```java
// Create underlying client
AsyncLLMClient openAIClient = new AsyncOpenAIClient(apiKey, model, maxTokens, temperature);

// Wrap with batching
BatchingLLMClient batchingClient = new BatchingLLMClient(openAIClient, config);
batchingClient.start();

// Use batching client
CompletableFuture<LLMResponse> response = batchingClient.submit(
    BatchingRequest.create("Generate a task plan", params, RequestPriority.NORMAL)
);
```

### Integration with TaskPlanner

Update `TaskPlanner` to use batching:

```java
public class TaskPlanner {

    private BatchingLLMClient batchingClient;

    public CompletableFuture<ParsedResponse> planTasksAsync(ForemanEntity foreman,
                                                            String command) {
        // Create request
        Map<String, Object> params = buildParams(foreman);

        BatchingRequest request = BatchingRequest.create(
            buildPrompt(foreman, command),
            params,
            BatchingRequest.RequestPriority.URGENT // User-initiated
        );

        // Submit to batching client
        return batchingClient.submit(request)
            .thenApply(response -> parseResponse(response.getContent()))
            .exceptionally(error -> {
                LOGGER.error("Batched request failed", error);
                return null;
            });
    }

    public CompletableFuture<String> submitBackgroundTask(ForemanEntity foreman,
                                                          String task) {
        BatchingRequest request = BatchingRequest.create(
            task,
            buildContext(foreman),
            BatchingRequest.RequestPriority.LOW // Background
        );

        return batchingClient.submit(request)
            .thenApply(LLMResponse::getContent);
    }
}
```

### Integration with Existing Batching

The new design replaces or enhances the existing `PromptBatcher`:

```java
// Option 1: Replace existing batching
public class TaskPlanner {
    private BatchingLLMClient batchingClient; // New implementation

    public BatchingLLMClient getBatchingClient() {
        if (batchingClient == null) {
            batchingClient = new BatchingLLMClient(asyncOpenAIClient,
                BatchingConfig.defaults());
            batchingClient.start();
        }
        return batchingClient;
    }
}

// Option 2: Wrap existing batching for backward compatibility
public class BatchingLLMClient {
    private final PromptBatcher legacyBatcher; // Keep existing

    // Delegate to legacy batcher for specific features
    public CompletableFuture<String> submitBackgroundPrompt(...) {
        return legacyBatcher.submitBackgroundPrompt(...);
    }
}
```

---

## Implementation Phases

### Phase 1: Core Infrastructure (Week 1)

**Tasks**:
1. Create `BatchingConfig` class
2. Create `BatchingRequest` class with priority enum
3. Implement `RequestQueue` with `PriorityBlockingQueue`
4. Create `BatchContainer` for holding batched requests
5. Implement `BatchAccumulator` state machine
6. Set up thread pool configuration

**Deliverables**:
- Configuration classes
- Request queuing system
- Basic batch accumulation logic

### Phase 2: Batch Processing (Week 2)

**Tasks**:
1. Implement `BatchCompiler` for merging requests
2. Create `BatchDispatcher` for sending batches
3. Implement time-window-based batching logic
4. Add size-based batch triggers
5. Implement max wait time enforcement
6. Create response parsing for batched responses

**Deliverables**:
- Working batch compilation
- Time-window batching
- Size-based batching
- Response routing to individual callbacks

### Phase 3: Error Handling and Resilience (Week 3)

**Tasks**:
1. Implement `BatchErrorHandler`
2. Add retry logic with exponential backoff
3. Integrate with circuit breaker
4. Implement graceful shutdown
5. Add request timeout handling
6. Handle partial batch failures

**Deliverables**:
- Complete error handling
- Retry mechanism
- Graceful shutdown
- Circuit breaker integration

### Phase 4: Metrics and Monitoring (Week 4)

**Tasks**:
1. Implement `BatchMetrics` collection
2. Create efficiency calculations
3. Add latency tracking
4. Implement metrics reporting
5. Create debugging/monitoring endpoints
6. Add log messages for key events

**Deliverables**:
- Complete metrics system
- Efficiency tracking
- Monitoring integration

### Phase 5: Integration and Testing (Week 5)

**Tasks**:
1. Integrate with `TaskPlanner`
2. Update configuration in `MineWrightConfig`
3. Write unit tests for core components
4. Write integration tests
5. Performance testing
6. Load testing with multiple concurrent requests

**Deliverables**:
- Full integration
- Test suite
- Performance benchmarks

### Phase 6: Documentation and Polish (Week 6)

**Tasks**:
1. Complete JavaDoc documentation
2. Create usage examples
3. Write troubleshooting guide
4. Code review and refactoring
5. Final testing and bug fixes
6. Release preparation

**Deliverables**:
- Complete documentation
- Example code
- Production-ready implementation

---

## Appendix

### Example Usage

```java
// Initialize batching client
AsyncLLMClient underlyingClient = new AsyncOpenAIClient(apiKey, model, maxTokens, temp);
BatchingLLMClient batchingClient = new BatchingLLMClient(underlyingClient,
    BatchingConfig.defaults());
batchingClient.start();

// Submit a user request
CompletableFuture<LLMResponse> userResponse = batchingClient.submitUrgent(
    "Build a house at coordinates 100, 64, 200",
    Map.of("foremanName", "Steve")
);

// Submit background tasks
for (String task : backgroundTasks) {
    batchingClient.submitBackground(task, Map.of("taskId", task.getId()));
}

// Check metrics
BatchMetrics metrics = batchingClient.getMetrics();
System.out.println("Efficiency: " + metrics.getEfficiencyScore());
System.out.println("Avg Latency: " + metrics.getAverageLatency() + "ms");

// Shutdown gracefully
batchingClient.stop(30000); // Wait up to 30 seconds for pending requests
```

### Performance Considerations

1. **Memory**: Each queued request holds the prompt string. With large prompts and deep queues, memory usage can be significant. Monitor queue size.

2. **Thread Pool Sizing**:
   - Too few threads: Batches wait too long to be sent
   - Too many threads: Context switching overhead
   - Recommendation: 2-4 dispatcher threads per CPU core

3. **Batch Size**:
   - Larger batches = fewer API calls but higher latency
   - Smaller batches = lower latency but more API calls
   - Optimal depends on rate limits and latency requirements

4. **Time Window**:
   - Shorter windows = lower latency, smaller batches
   - Longer windows = higher latency, larger batches
   - Default 100ms balances latency and batching

### Troubleshooting

**Problem**: Requests timing out
- **Cause**: Batch window too long or max batch size too high
- **Solution**: Reduce `batchWindowMs` or `maxBatchSize`

**Problem**: High rate of 429 errors
- **Cause**: Batches not reducing API calls enough
- **Solution**: Increase `batchWindowMs` to accumulate larger batches

**Problem**: Memory usage increasing
- **Cause**: Queue backing up due to slow LLM responses
- **Solution**: Reduce `queueCapacity`, add more dispatcher threads, or check LLM health

**Problem**: Low batching efficiency
- **Cause**: Request rate too low for effective batching
- **Solution**: Consider if batching is needed for this use case, or increase `batchWindowMs`

---

## Summary

This enhanced `BatchingLLMClient` design provides:

1. **Flexible Configuration**: Tune batching behavior for your needs
2. **Thread Safety**: Safe for concurrent access from multiple threads
3. **Priority Handling**: User requests get priority over background tasks
4. **Smart Batching**: Time-window and size-based batch triggers
5. **Error Resilience**: Retry logic and graceful degradation
6. **Observable Metrics**: Track efficiency and performance
7. **Clean Integration**: Works with existing `AsyncLLMClient` interface

The implementation is divided into 6 phases over 6 weeks, with each phase building on the previous one. The design prioritizes correctness and thread safety, followed by performance optimization.
