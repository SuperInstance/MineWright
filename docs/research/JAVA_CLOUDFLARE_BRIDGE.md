# Java-to-Cloudflare Bridge Architecture

**Author:** Orchestrator Research Team
**Date:** 2026-02-27
**Status:** Design Document
**Version:** 1.0

---

## Executive Summary

This document describes the architecture for integrating Cloudflare Workers as an edge computing layer for the MineWright Minecraft mod. The bridge enables tactical decision-making to be offloaded to Cloudflare's global network while maintaining full offline fallback capabilities.

**Key Design Goals:**
- Sub-20ms response time for tactical calls (within Minecraft's 50ms tick budget)
- Complete offline fallback (no game disruption)
- Thread-safe, non-blocking integration with existing ActionExecutor
- Secure API communication with proper authentication
- Graceful degradation when edge services are unavailable

---

## 1. Bridge Architecture Overview

### 1.1 High-Level Architecture

```
Minecraft Server (Java)                Cloudflare Edge (JavaScript)
┌──────────────────────┐              ┌──────────────────────────────┐
│                      │              │                              │
│  ForemanEntity       │              │  Worker: Tactical AI         │
│  └─ ActionExecutor   │◄──HTTP──────►│  ├─ Pathfinding optimization │
│      └─ BaseAction   │   WebSocket  │  ├─ Combat decisions        │
│          └─ tick()   │              │  └─ Resource allocation     │
│                      │              │                              │
│  CloudflareBridge    │              │  Worker: State Sync         │
│  ├─ HttpClient       │◄───────┬─────│  ├─ World state aggregation │
│  ├─ WebSocketClient  │        │     │  ├─ Multi-agent coordination│
│  └─ OfflineQueue     │        │     │  └─ Conflict resolution    │
│                      │        │     │                              │
│  LocalFallback       │        │     │  KV/D1: Persistence         │
│  └─ LLM Clients      │        │     │  ├─ Agent state cache       │
│                      │        │     │  ├─ Shared memory          │
└──────────────────────┘        │     │  └─ Task queues            │
                               │     └──────────────────────────────┘
                               │
                      Offline  │
                      Fallback │
```

### 1.2 Request Types

| Request Type | Purpose | Latency Target | Fallback Strategy |
|--------------|---------|----------------|-------------------|
| **Tactical** | Real-time decisions (combat, pathfinding) | <20ms | Local decision tree |
| **Strategic** | Task planning, multi-agent coordination | <500ms | Local LLM (OpenAI/Groq) |
| **Sync** | State persistence, cross-server coordination | <1s | Queue for retry |
| **Telemetry** | Analytics, logging (fire-and-forget) | N/A | Drop if offline |

### 1.3 Communication Patterns

**HTTP/REST (Primary)**
```java
// Synchronous-with-timeout pattern
TacticalResponse response = bridge.sendTacticalRequest(request)
    .timeout(20, TimeUnit.MILLISECONDS)
    .fallback(localFallback::decide)
    .execute();
```

**WebSocket (Secondary - for streaming updates)**
```java
// Bi-directional streaming for state updates
websocket.connect()
    .onMessage(StateUpdate::apply)
    .onReconnect(() -> syncFullState())
    .keepAlive(30, TimeUnit.SECONDS);
```

**Fire-and-Forget (Telemetry)**
```java
// Non-blocking, best-effort delivery
bridge.sendTelemetry(event); // Returns immediately
```

---

## 2. Existing Code Integration Points

### 2.1 ActionExecutor Integration

**Location:** `src/main/java/com/minewright/action/ActionExecutor.java`

**Integration Strategy:** Inject CloudflareBridge as an optional enhancement layer.

```java
public class ActionExecutor {
    private final ForemanEntity foreman;
    private final CloudflareBridge cloudflareBridge; // NEW
    private final ActionContext actionContext;
    private final InterceptorChain interceptorChain;

    public ActionExecutor(ForemanEntity foreman) {
        this.foreman = foreman;
        this.cloudflareBridge = CloudflareBridge.getInstance();

        // Initialize existing components
        this.eventBus = new SimpleEventBus();
        this.stateMachine = new AgentStateMachine(eventBus, foreman.getSteveName());
        this.interceptorChain = new InterceptorChain();

        // NEW: Add Cloudflare interceptor to chain
        if (cloudflareBridge.isEnabled()) {
            interceptorChain.addInterceptor(new CloudflareTacticalInterceptor(cloudflareBridge));
        }

        // ... rest of initialization
    }

    public void tick() {
        ticksSinceLastAction++;

        // Check if async planning is complete
        if (isPlanning && planningFuture != null && planningFuture.isDone()) {
            // ... existing planning completion logic
        }

        if (currentAction != null) {
            if (currentAction.isComplete()) {
                ActionResult result = currentAction.getResult();

                // NEW: Send result telemetry to Cloudflare (non-blocking)
                cloudflareBridge.sendTelemetryAsync(
                    TelemetryEvent.actionCompleted(foreman.getSteveName(), result)
                );

                // ... existing completion logic
            } else {
                // NEW: Check if action needs tactical decision
                if (currentAction instanceof TacticalAction) {
                    ((TacticalAction) currentAction).consultCloudflare(cloudflareBridge);
                }

                currentAction.tick();
                return;
            }
        }

        // ... rest of tick logic
    }
}
```

### 2.2 AgentStateMachine Integration

**Location:** `src/main/java/com/minewright/execution/AgentStateMachine.java`

**Integration Strategy:** Extend state transitions to sync with Cloudflare.

```java
public class AgentStateMachine {
    // ... existing code

    public boolean transitionTo(AgentState targetState, String reason) {
        // ... existing transition logic

        if (currentState.compareAndSet(fromState, targetState)) {
            LOGGER.info("[{}] State transition: {} → {}", agentId, fromState, targetState);

            if (eventBus != null) {
                eventBus.publish(new StateTransitionEvent(agentId, fromState, targetState, reason));
            }

            // NEW: Sync state to Cloudflare (fire-and-forget)
            CloudflareBridge.getInstance().syncStateAsync(
                agentId,
                targetState,
                reason
            );

            return true;
        }
        return false;
    }
}
```

### 2.3 InterceptorChain Integration

**Location:** `src/main/java/com/minewright/execution/InterceptorChain.java`

**Integration Strategy:** Add CloudflareTacticalInterceptor to the chain.

```java
// In ActionExecutor constructor
InterceptorChain chain = new InterceptorChain();

// Add interceptors in priority order
chain.addInterceptor(new CloudflareTacticalInterceptor(cloudflareBridge)); // Priority: 100
chain.addInterceptor(new LoggingInterceptor());                            // Priority: 50
chain.addInterceptor(new MetricsInterceptor());                            // Priority: 40
chain.addInterceptor(new EventPublishingInterceptor(eventBus, agentId));  // Priority: 30
```

**New Interceptor:**

```java
public class CloudflareTacticalInterceptor implements ActionInterceptor {
    private final CloudflareBridge bridge;

    public CloudflareTacticalInterceptor(CloudflareBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public boolean beforeAction(BaseAction action, ActionContext context) {
        // Check if action can benefit from tactical optimization
        if (action instanceof TacticalAction) {
            TacticalRequest request = TacticalRequest.fromAction(action, context);
            Optional<TacticalResponse> response = bridge.tryTacticalDecision(request);

            if (response.isPresent()) {
                // Apply tactical optimizations
                response.get().applyTo(action);
                return true;
            }
        }
        return true; // Proceed with action even if Cloudflare unavailable
    }

    @Override
    public String getName() {
        return "CloudflareTacticalInterceptor";
    }

    @Override
    public int getPriority() {
        return 100; // Run before all other interceptors
    }
}
```

---

## 3. HTTP Client Implementation

### 3.1 Java 11+ HttpClient Configuration

```java
package com.minewright.cloudflare;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for Cloudflare Workers communication.
 *
 * <p><b>Thread Safety:</b> This class is thread-safe and uses a shared
 * HttpClient instance with connection pooling.</p>
 *
 * <p><b>Performance:</b></p>
 * <ul>
 *   <li>Connection pooling: Up to 20 concurrent connections</li>
 *   <li>Keep-alive: Reuses connections for 30 seconds</li>
 *   <li>HTTP/2: Multiplexing over single connection</li>
 *   <li>Timeouts: Configurable per request type</li>
 * </ul>
 */
public class CloudflareHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudflareHttpClient.class);

    // Shared HTTP client with connection pooling
    private static final HttpClient SHARED_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(10))
        .executor(Executors.newFixedThreadPool(20, new NamedThreadFactory("cloudflare-http")))
        .build();

    private final String baseUrl;
    private final String apiToken;
    private final int tacticalTimeoutMs;
    private final int strategicTimeoutMs;

    public CloudflareHttpClient(CloudflareConfig config) {
        this.baseUrl = config.getWorkerUrl().endsWith("/")
            ? config.getWorkerUrl()
            : config.getWorkerUrl() + "/";
        this.apiToken = config.getApiToken();
        this.tacticalTimeoutMs = config.getTacticalTimeoutMs();
        this.strategicTimeoutMs = config.getStrategicTimeoutMs();
    }

    /**
     * Sends a tactical request with strict timeout.
     *
     * <p><b>Latency Target:</b> <20ms to fit within Minecraft tick budget</p>
     *
     * @param request Tactical request
     * @return CompletableFuture with response
     */
    public CompletableFuture<TacticalResponse> sendTacticalAsync(TacticalRequest request) {
        return sendAsync("/tactical", request, Duration.ofMillis(tacticalTimeoutMs))
            .thenApply(this::parseTacticalResponse)
            .exceptionally(throwable -> {
                LOGGER.debug("Tactical request failed: {}", throwable.getMessage());
                return null; // Signal to use fallback
            });
    }

    /**
     * Sends a strategic request with relaxed timeout.
     *
     * <p><b>Latency Target:</b> <500ms for complex planning tasks</p>
     *
     * @param request Strategic request
     * @return CompletableFuture with response
     */
    public CompletableFuture<StrategicResponse> sendStrategicAsync(StrategicRequest request) {
        return sendAsync("/strategic", request, Duration.ofMillis(strategicTimeoutMs))
            .thenApply(this::parseStrategicResponse);
    }

    /**
     * Sends a state sync request (fire-and-forget).
     *
     * @param state State to sync
     */
    public void syncStateAsync(AgentState state) {
        sendAsync("/sync", state, Duration.ofMillis(500))
            .whenComplete((response, throwable) -> {
                if (throwable != null) {
                    LOGGER.debug("State sync failed (will retry): {}", throwable.getMessage());
                }
            });
    }

    /**
     * Sends telemetry (fire-and-forget, no retries).
     *
     * @param event Telemetry event
     */
    public void sendTelemetryAsync(TelemetryEvent event) {
        // Don't wait for response, don't retry
        sendAsync("/telemetry", event, Duration.ofMillis(200))
            .whenComplete((response, throwable) -> {
                // Silently ignore failures
            });
    }

    private <T, R> CompletableFuture<R> sendAsync(
            String path,
            T requestBody,
            Duration timeout) {

        try {
            String jsonBody = JsonMapper.toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiToken)
                .header("User-Agent", "MineWright/1.0")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            return SHARED_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> (R) JsonMapper.fromJson(body, getResponseClass(path)));

        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

### 3.2 Request/Response Models

```java
package com.minewright.cloudflare.model;

import java.util.Map;

/**
 * Tactical decision request for real-time game decisions.
 *
 * <p>Used for combat, pathfinding, and immediate action optimization.</p */
public record TacticalRequest(
    String agentId,
    String actionType,
    Map<String, Object> context,
    Position agentPosition,
    Position targetPosition,
    long timestamp
) {
    public static TacticalRequest fromAction(BaseAction action, ActionContext ctx) {
        return new TacticalRequest(
            action.getForeman().getSteveName(),
            action.getClass().getSimpleName(),
            buildContext(action, ctx),
            Position.fromEntity(action.getForeman()),
            extractTargetPosition(action),
            System.currentTimeMillis()
        );
    }

    private static Map<String, Object> buildContext(BaseAction action, ActionContext ctx) {
        Map<String, Object> context = new HashMap<>();
        context.put("state", ctx.getCurrentState().name());
        context.put("health", action.getForeman().getHealth());
        context.put("inventory", action.getForeman().getInventory().snapshot());
        return context;
    }
}

/**
 * Tactical response with optimization hints.
 */
public record TacticalResponse(
    boolean success,
    String optimizationHint,
    Map<String, Object> parameters,
    long processingTimeMs
) {
    public void applyTo(BaseAction action) {
        if (success && optimizationHint != null) {
            action.applyOptimization(optimizationHint, parameters);
        }
    }
}

/**
 * Strategic request for complex planning.
 */
public record StrategicRequest(
    String agentId,
    String goal,
    List<Task> currentTasks,
    WorldState worldState,
    long timestamp
) {}

/**
 * Strategic response with task optimizations.
 */
public record StrategicResponse(
    List<Task> optimizedTasks,
    String reasoning,
    long processingTimeMs
) {}

/**
 * Position record for spatial queries.
 */
public record Position(int x, int y, int z) {
    public static Position fromEntity(ForemanEntity entity) {
        BlockPos pos = entity.blockPosition();
        return new Position(pos.getX(), pos.getY(), pos.getZ());
    }
}
```

### 3.3 Timeout Handling

```java
/**
 * Timeout strategies by request type.
 */
public enum RequestType {
    TACTICAL(20, Duration.ofMillis(50)),      // 20ms target, 50ms absolute max
    STRATEGIC(200, Duration.ofMillis(2000)),  // 200ms target, 2s max
    SYNC(500, Duration.ofMillis(2000)),       // 500ms target, 2s max
    TELEMETRY(0, Duration.ofMillis(500));     // Fire-and-forget, 500ms max

    private final int targetLatencyMs;
    private final Duration timeout;

    RequestType(int targetLatencyMs, Duration timeout) {
        this.targetLatencyMs = targetLatencyMs;
        this.timeout = timeout;
    }
}

/**
 * Timeout-aware request wrapper.
 */
public class TimeoutAwareRequest<T> {
    private final RequestType type;
    private final T request;
    private final Supplier<CompletableFuture<T>> executor;

    public CompletableFuture<T> executeWithFallback(Supplier<T> fallback) {
        return executor.get()
            .completeOnTimeout(null, 0, TimeUnit.MILLISECONDS) // Immediate timeout if too slow
            .thenApply(result -> result != null ? result : fallback.get());
    }
}
```

### 3.4 Retry Logic with Exponential Backoff

```java
package com.minewright.cloudflare.resilience;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Retry logic with exponential backoff for Cloudflare requests.
 *
 * <p><b>Retry Strategy:</b></p>
 * <ul>
 *   <li>Initial delay: 50ms</li>
 *   <li>Backoff multiplier: 2.0</li>
 *   <li>Max retries: 3</li>
 *   <li>Max delay: 500ms</li>
 * </ul>
 */
public class RetryHandler {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 50;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final long MAX_DELAY_MS = 500;

    private final ScheduledExecutorService scheduler;

    public RetryHandler() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            new NamedThreadFactory("cloudflare-retry")
        );
    }

    /**
     * Executes a request with retry logic.
     *
     * @param requestSupplier Function that executes the request
     * @param shouldRetry Predicate to determine if retry is appropriate
     * @param <T> Response type
     * @return CompletableFuture with response or null if all retries exhausted
     */
    public <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> requestSupplier,
            Predicate<Throwable> shouldRetry) {

        CompletableFuture<T> result = new CompletableFuture<>();
        executeWithRetry(requestSupplier, shouldRetry, 0, result);
        return result;
    }

    private <T> void executeWithRetry(
            Supplier<CompletableFuture<T>> requestSupplier,
            Predicate<Throwable> shouldRetry,
            int attempt,
            CompletableFuture<T> result) {

        requestSupplier.get().whenComplete((response, throwable) -> {
            if (throwable == null) {
                result.complete(response);
            } else if (attempt < MAX_RETRIES && shouldRetry.test(throwable)) {
                long delayMs = calculateDelay(attempt);
                LOGGER.debug("Request failed (attempt {}), retrying in {}ms: {}",
                    attempt + 1, delayMs, throwable.getMessage());

                scheduler.schedule(() -> {
                    executeWithRetry(requestSupplier, shouldRetry, attempt + 1, result);
                }, delayMs, TimeUnit.MILLISECONDS);
            } else {
                result.completeExceptionally(throwable);
            }
        });
    }

    private long calculateDelay(int attempt) {
        long delay = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attempt));
        return Math.min(delay, MAX_DELAY_MS);
    }

    /**
     * Checks if an exception is retryable.
     */
    public static boolean isRetryable(Throwable throwable) {
        if (throwable instanceof java.net.SocketTimeoutException) {
            return true; // Network timeout
        }
        if (throwable instanceof java.net.ConnectException) {
            return true; // Connection refused
        }
        if (throwable instanceof java.io.IOException) {
            return true; // Generic IO error
        }
        if (throwable instanceof HttpException httpEx) {
            return httpEx.statusCode() >= 500 || httpEx.statusCode() == 429;
        }
        return false;
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
```

### 3.5 Connection Pooling

The Java 11 HttpClient automatically manages connection pooling:

```java
// Connection pool configuration (implicit in HttpClient)
HttpClient.newBuilder()
    .executor(Executors.newFixedThreadPool(
        20,  // Max concurrent connections
        new NamedThreadFactory("cloudflare-http")
    ))
    .connectTimeout(Duration.ofSeconds(10))
    .build();

// Connection behavior:
// - HTTP/2: Single TCP connection, multiplexed requests
// - HTTP/1.1: Up to 20 concurrent connections
// - Keep-alive: Connections reused for 30 seconds
// - Idle timeout: Closed after 60 seconds of inactivity
```

---

## 4. State Synchronization

### 4.1 Local-to-Edge State Sync Pattern

```java
package com.minewright.cloudflare.sync;

/**
 * Manages state synchronization between Minecraft and Cloudflare.
 *
 * <p><b>Sync Strategy:</b></p>
 * <ul>
 *   <li><b>Optimistic Sync:</b> Assume local state is authoritative</li>
 *   <li><b>Conflict Detection:</b> Version vectors + last-write-wins</li>
 *   <li><b>Delta Updates:</b> Only send changes, not full state</li>
 *   <li><b>Batching:</b> Accumulate changes, send in batches</li>
 * </ul>
 */
public class StateSynchronizer {

    private final CloudflareHttpClient httpClient;
    private final StateDeltaCollector deltaCollector;
    private final VersionVector versionVector;
    private final OfflineQueue offlineQueue;

    @Inject
    public StateSynchronizer(CloudflareHttpClient httpClient) {
        this.httpClient = httpClient;
        this.deltaCollector = new StateDeltaCollector();
        this.versionVector = new VersionVector();
        this.offlineQueue = new OfflineQueue();
    }

    /**
     * Records a state change locally.
     *
     * @param agentId Agent that changed
     * @param newState New state
     */
    public void recordStateChange(String agentId, AgentState newState) {
        // Record locally
        StateDelta delta = deltaCollector.recordChange(agentId, newState);

        // Sync to edge asynchronously
        syncDeltaAsync(delta)
            .whenComplete((success, throwable) -> {
                if (throwable != null || !success) {
                    // Queue for retry when offline
                    offlineQueue.enqueue(delta);
                }
            });
    }

    /**
     * Syncs a state delta to Cloudflare.
     *
     * @param delta State delta to sync
     * @return CompletableFuture with success status
     */
    private CompletableFuture<Boolean> syncDeltaAsync(StateDelta delta) {
        return httpClient.sendAsync("/sync/delta", delta, Duration.ofMillis(500))
            .thenApply(response -> {
                SyncAck ack = JsonMapper.fromJson(response, SyncAck.class);
                if (ack.accepted()) {
                    versionVector.update(delta.getAgentId(), ack.getVersion());
                    deltaCollector.acknowledge(delta);
                    return true;
                } else {
                    LOGGER.warn("Delta rejected by server: {}", ack.getReason());
                    return false;
                }
            })
            .exceptionally(throwable -> {
                LOGGER.debug("Failed to sync delta: {}", throwable.getMessage());
                return false;
            });
    }

    /**
     * Performs a full state sync (called on reconnection).
     *
     * @param agentId Agent to sync
     */
    public void fullSync(String agentId) {
        FullState localState = deltaCollector.getFullState(agentId);

        httpClient.sendAsync("/sync/full", localState, Duration.ofSeconds(2))
            .whenComplete((response, throwable) -> {
                if (throwable != null) {
                    LOGGER.warn("Full sync failed for agent {}: {}",
                        agentId, throwable.getMessage());
                    return;
                }

                FullState remoteState = JsonMapper.fromJson(response, FullState.class);
                resolveConflicts(agentId, localState, remoteState);
            });
    }

    /**
     * Resolves conflicts between local and remote state.
     *
     * <p><b>Conflict Resolution Strategy:</b> Last-write-wins with version vectors</p>
     */
    private void resolveConflicts(String agentId, FullState local, FullState remote) {
        long localVersion = versionVector.getVersion(agentId);
        long remoteVersion = remote.getVersion();

        if (remoteVersion > localVersion) {
            // Remote is newer, accept remote state
            LOGGER.info("Accepting remote state for {} (v{} > v{})",
                agentId, remoteVersion, localVersion);
            deltaCollector.applyRemoteState(agentId, remote);
            versionVector.update(agentId, remoteVersion);
        } else {
            // Local is newer or same, push local state
            LOGGER.info("Pushing local state for {} (v{} >= v{})",
                agentId, localVersion, remoteVersion);
            syncDeltaAsync(StateDelta.fromFull(local));
        }
    }
}
```

### 4.2 Conflict Resolution

```java
package com.minewright.cloudflare.sync;

/**
 * Version vector for tracking state versions across replicas.
 *
 * <p><b>Version Vector Properties:</b></p>
 * <ul>
 *   <li>Each replica has a monotonically increasing version</li>
 *   <li>Comparisons determine which state is newer</li>
 *   <li>Can detect concurrent modifications</li>
 * </ul>
 */
public class VersionVector {
    private final ConcurrentMap<String, Long> versions = new ConcurrentHashMap<>();

    /**
     * Updates the version for an agent.
     *
     * @param agentId Agent ID
     * @param version New version
     */
    public void update(String agentId, long version) {
        versions.compute(agentId, (id, current) ->
            current == null ? version : Math.max(current, version)
        );
    }

    /**
     * Gets the current version for an agent.
     *
     * @param agentId Agent ID
     * @return Current version, or 0 if not tracked
     */
    public long getVersion(String agentId) {
        return versions.getOrDefault(agentId, 0L);
    }

    /**
     * Compares two version numbers.
     *
     * @return Positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    public static int compare(long v1, long v2) {
        return Long.compare(v1, v2);
    }
}

/**
 * State delta representing changes to agent state.
 */
public record StateDelta(
    String agentId,
    long version,
    long timestamp,
    Map<String, Object> changes,
    String changeType
) {
    public static StateDelta fromFull(FullState state) {
        return new StateDelta(
            state.getAgentId(),
            state.getVersion(),
            System.currentTimeMillis(),
            state.getAllFields(),
            "FULL"
        );
    }
}
```

### 4.3 Offline Queue Buffering

```java
package com.minewright.cloudflare.sync;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Queue for buffering state changes when offline.
 *
 * <p><b>Queue Behavior:</b></p>
 * <ul>
 *   <li>Max size: 10,000 entries (configurable)</li>
 *   <li>Persistence: Optional disk-based queue for durability</li>
 *   <li>Drain: Sent on reconnection</li>
 * </ul>
 */
public class OfflineQueue {

    private static final int MAX_QUEUE_SIZE = 10_000;
    private final BlockingQueue<StateDelta> queue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final Path persistentFile;

    public OfflineQueue() {
        this.persistentFile = Paths.get("config/minewright/offline_queue.bin");
        loadFromDisk();
    }

    /**
     * Enqueues a delta for later sync.
     *
     * @param delta Delta to enqueue
     * @return true if enqueued, false if queue is full
     */
    public boolean enqueue(StateDelta delta) {
        boolean added = queue.offer(delta);
        if (!added) {
            LOGGER.warn("Offline queue full, dropping delta for {}", delta.agentId());
        }
        persistToDisk();
        return added;
    }

    /**
     * Drains the queue, sending all deltas to Cloudflare.
     *
     * @param httpClient Client to send deltas
     * @return Number of deltas successfully sent
     */
    public int drain(CloudflareHttpClient httpClient) {
        int sent = 0;
        StateDelta delta;

        while ((delta = queue.poll()) != null) {
            httpClient.sendAsync("/sync/delta", delta, Duration.ofMillis(500))
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        // Re-queue on failure
                        queue.offer(delta);
                    }
                });
            sent++;
        }

        persistToDisk();
        return sent;
    }

    /**
     * Returns the current queue size.
     */
    public int size() {
        return queue.size();
    }

    private void persistToDisk() {
        // TODO: Implement serialization to persistentFile
    }

    private void loadFromDisk() {
        // TODO: Load from persistentFile if exists
    }
}
```

### 4.4 Delta Updates vs Full Sync

```java
/**
 * Determines sync strategy based on change magnitude.
 */
public class SyncStrategy {

    private static final int DELTA_SIZE_THRESHOLD = 10; // Number of fields

    /**
     * Decides between delta sync and full sync.
     *
     * @param changes Map of changed fields
     * @return Sync strategy to use
     */
    public static SyncMode determineStrategy(Map<String, Object> changes) {
        if (changes.size() > DELTA_SIZE_THRESHOLD) {
            return SyncMode.FULL;
        }

        // Check if any field value is large
        for (Object value : changes.values()) {
            if (value instanceof Collection && ((Collection<?>) value).size() > 100) {
                return SyncMode.FULL;
            }
        }

        return SyncMode.DELTA;
    }

    public enum SyncMode {
        DELTA,  // Only send changed fields
        FULL    // Send entire state
    }
}
```

---

## 5. Configuration

### 5.1 MineWrightConfig.java Updates

**Location:** `src/main/java/com/minewright/config/MineWrightConfig.java`

```java
public class MineWrightConfig {
    // ... existing config fields

    // ========== Cloudflare Configuration ==========

    public static final ForgeConfigSpec.BooleanValue CLOUDFLARE_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> CLOUDFLARE_WORKER_URL;
    public static final ForgeConfigSpec.ConfigValue<String> CLOUDFLARE_API_TOKEN;
    public static final ForgeConfigSpec.IntValue CLOUDFLARE_TACTICAL_TIMEOUT_MS;
    public static final ForgeConfigSpec.IntValue CLOUDFLARE_STRATEGIC_TIMEOUT_MS;
    public static final ForgeConfigSpec.BooleanValue CLOUDFLARE_OFFLINE_FALLBACK;
    public static final ForgeConfigSpec.IntValue CLOUDFLARE_OFFLINE_QUEUE_MAX_SIZE;
    public static final ForgeConfigSpec.BooleanValue CLOUDFLARE_TELEMETRY_ENABLED;
    public static final ForgeConfigSpec.IntValue CLOUDFLARE_MAX_RETRIES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // ... existing config sections

        // Cloudflare Configuration Section
        builder.comment("Cloudflare Workers Integration").push("cloudflare");

        CLOUDFLARE_ENABLED = builder
            .comment("Enable Cloudflare Workers integration for edge AI processing")
            .define("enabled", false);

        CLOUDFLARE_WORKER_URL = builder
            .comment("Cloudflare Worker URL (e.g., https://minewright.your-subdomain.workers.dev)")
            .define("workerUrl", "");

        CLOUDFLARE_API_TOKEN = builder
            .comment("API token for Cloudflare Worker authentication")
            .define("apiToken", "");

        CLOUDFLARE_TACTICAL_TIMEOUT_MS = builder
            .comment("Timeout for tactical requests (milliseconds). Recommend 20ms for real-time decisions")
            .defineInRange("tacticalTimeoutMs", 20, 10, 100);

        CLOUDFLARE_STRATEGIC_TIMEOUT_MS = builder
            .comment("Timeout for strategic requests (milliseconds). Recommend 500ms for planning")
            .defineInRange("strategicTimeoutMs", 500, 200, 5000);

        CLOUDFLARE_OFFLINE_FALLBACK = builder
            .comment("Use local LLM when Cloudflare is unavailable")
            .define("offlineFallback", true);

        CLOUDFLARE_OFFLINE_QUEUE_MAX_SIZE = builder
            .comment("Maximum number of state changes to queue while offline")
            .defineInRange("offlineQueueMaxSize", 10000, 1000, 100000);

        CLOUDFLARE_TELEMETRY_ENABLED = builder
            .comment("Send telemetry to Cloudflare for analytics")
            .define("telemetryEnabled", false);

        CLOUDFLARE_MAX_RETRIES = builder
            .comment("Maximum retry attempts for failed requests")
            .defineInRange("maxRetries", 3, 0, 10);

        builder.pop(); // End cloudflare section

        SPEC = builder.build();
    }

    /**
     * Validates Cloudflare configuration.
     */
    public static boolean validateCloudflareConfig() {
        if (!CLOUDFLARE_ENABLED.get()) {
            LOGGER.info("Cloudflare integration disabled");
            return true;
        }

        boolean isValid = true;

        String workerUrl = CLOUDFLARE_WORKER_URL.get();
        if (workerUrl == null || workerUrl.trim().isEmpty()) {
            LOGGER.error("Cloudflare enabled but workerUrl is not configured!");
            isValid = false;
        } else if (!isValidUrl(workerUrl)) {
            LOGGER.error("Invalid Cloudflare Worker URL: {}", workerUrl);
            isValid = false;
        }

        String apiToken = CLOUDFLARE_API_TOKEN.get();
        if (apiToken == null || apiToken.trim().isEmpty()) {
            LOGGER.error("Cloudflare enabled but apiToken is not configured!");
            isValid = false;
        } else if (apiToken.length() < 32) {
            LOGGER.warn("API token seems unusually short (expected >= 32 chars)");
        }

        if (isValid) {
            LOGGER.info("Cloudflare integration configured: {} (timeout: {}ms tactical, {}ms strategic)",
                workerUrl, CLOUDFLARE_TACTICAL_TIMEOUT_MS.get(), CLOUDFLARE_STRATEGIC_TIMEOUT_MS.get());
        }

        return isValid;
    }

    private static boolean isValidUrl(String url) {
        try {
            new URI(url).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 5.2 Config File Example

**Location:** `config/minewright-common.toml`

```toml
# Cloudflare Workers Integration
[cloudflare]
    # Enable Cloudflare Workers for edge AI processing
    enabled = false

    # Cloudflare Worker URL (get from Cloudflare dashboard)
    workerUrl = "https://minewright-production.your-account.workers.dev"

    # API token for authentication (generate in Cloudflare dashboard)
    apiToken = "your_api_token_here_min_32_chars"

    # Timeout for real-time tactical decisions (combat, pathfinding)
    # Lower = faster fallback to local, Higher = more time for edge processing
    tacticalTimeoutMs = 20

    # Timeout for strategic planning (task assignment, multi-agent coordination)
    strategicTimeoutMs = 500

    # Use local LLM (OpenAI/Groq) when Cloudflare is unavailable
    offlineFallback = true

    # Maximum state changes to queue while offline
    offlineQueueMaxSize = 10000

    # Send anonymous telemetry for analytics
    telemetryEnabled = false

    # Maximum retry attempts for failed requests
    maxRetries = 3
```

---

## 6. Latency Budget

### 6.1 Minecraft Tick Constraints

```
Minecraft Server Tick: 50ms (20 ticks/second)
├── World Processing: ~20ms
├── Entity AI: ~10ms
├── Network I/O: ~10ms
└── Headroom: ~10ms

Therefore: Cloudflare calls MUST complete in <20ms to avoid tick spillover
```

### 6.2 Latency Budget Breakdown

| Operation | Target | Max | Action on Exceed |
|-----------|--------|-----|------------------|
| **Tactical Call** | 10ms | 20ms | Immediate fallback to local |
| **Strategic Call** | 200ms | 500ms | Continue with local LLM, use edge result when ready |
| **State Sync** | 200ms | 1000ms | Queue for retry, continue offline |
| **Telemetry** | N/A | 200ms | Drop if timeout, don't retry |

### 6.3 Latency Monitoring

```java
package com.minewright.cloudflare.metrics;

/**
 * Tracks latency metrics for Cloudflare requests.
 */
public class LatencyTracker {

    private final AtomicLong tacticalTotal = new AtomicLong(0);
    private final AtomicLong tacticalCount = new AtomicLong(0);
    private final AtomicLong tacticalTimeouts = new AtomicLong(0);

    private final AtomicLong strategicTotal = new AtomicLong(0);
    private final AtomicLong strategicCount = new AtomicLong(0);

    /**
     * Records a tactical request latency.
     *
     * @param latencyMs Actual latency in milliseconds
     * @param timedOut Whether the request timed out
     */
    public void recordTactical(long latencyMs, boolean timedOut) {
        if (timedOut) {
            tacticalTimeouts.incrementAndGet();
        } else {
            tacticalTotal.addAndGet(latencyMs);
            tacticalCount.incrementAndGet();
        }
    }

    /**
     * Gets the average tactical latency.
     *
     * @return Average latency in ms, or -1 if no successful requests
     */
    public double getAverageTacticalLatency() {
        long count = tacticalCount.get();
        if (count == 0) return -1;
        return (double) tacticalTotal.get() / count;
    }

    /**
     * Gets the tactical timeout rate.
     *
     * @return Timeout rate (0.0 to 1.0)
     */
    public double getTacticalTimeoutRate() {
        long timeouts = tacticalTimeouts.get();
        long count = tacticalCount.get() + timeouts;
        if (count == 0) return 0;
        return (double) timeouts / count;
    }

    /**
     * Checks if latency is within budget.
     *
     * @return true if average latency < 20ms and timeout rate < 10%
     */
    public boolean isLatencyHealthy() {
        double avgLatency = getAverageTacticalLatency();
        double timeoutRate = getTacticalTimeoutRate();

        return avgLatency >= 0 && avgLatency < 20 && timeoutRate < 0.1;
    }

    public void logMetrics() {
        LOGGER.info("Cloudflare Latency Metrics:");
        LOGGER.info("  Tactical: {}ms avg ({}% timeout)",
            String.format("%.2f", getAverageTacticalLatency()),
            String.format("%.1f", getTacticalTimeoutRate() * 100));
    }
}
```

### 6.4 Timeout Handling in ActionExecutor

```java
public void tick() {
    // ... existing code

    if (currentAction != null) {
        // Check if action needs tactical decision (non-blocking)
        if (currentAction instanceof TacticalAction tacticalAction) {
            // Try to get tactical decision from Cloudflare
            Optional<TacticalResponse> response = cloudflareBridge.tryTactical(
                TacticalRequest.fromAction(tacticalAction, actionContext),
                Duration.ofMillis(MineWrightConfig.CLOUDFLARE_TACTICAL_TIMEOUT_MS.get())
            );

            if (response.isPresent()) {
                // Apply optimization
                response.get().applyTo(tacticalAction);
            } // Else: proceed with local logic (fallback)
        }

        currentAction.tick();

        // Check if tick exceeded budget
        long tickTime = System.currentTimeMillis() - tickStartTime;
        if (tickTime > 40) { // Warn if approaching 50ms limit
            LOGGER.warn("Tick took {}ms for action {} (approaching tick budget)",
                tickTime, currentAction.getDescription());
        }
    }
}
```

---

## 7. Code Examples

### 7.1 CloudflareBridge.java

```java
package com.minewright.cloudflare;

import com.minewright.MineWrightMod;
import com.minewright.config.MineWrightConfig;
import com.minewright.cloudflare.model.*;
import com.minewright.cloudflare.sync.OfflineQueue;
import com.minewright.cloudflare.sync.StateSynchronizer;
import com.minewright.cloudflare.metrics.LatencyTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main bridge for communicating with Cloudflare Workers.
 *
 * <p><b>Singleton Pattern:</b> Use {@link #getInstance()} to get the shared instance.</p>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe.</p>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // Try to get tactical decision (non-blocking, optional result)
 * Optional&lt;TacticalResponse&gt; response = CloudflareBridge.getInstance()
 *     .tryTactical(request, Duration.ofMillis(20));
 *
 * if (response.isPresent()) {
 *     response.get().applyTo(action);
 * } else {
 *     // Fall back to local logic
 * }
 *
 * // Send telemetry (fire-and-forget)
 * CloudflareBridge.getInstance().sendTelemetryAsync(event);
 *
 * // Sync state (async, retries automatically)
 * CloudflareBridge.getInstance().syncStateAsync(agentId, state);
 * </pre>
 */
public class CloudflareBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudflareBridge.class);
    private static final CloudflareBridge INSTANCE = new CloudflareBridge();

    private final CloudflareHttpClient httpClient;
    private final StateSynchronizer stateSynchronizer;
    private final LatencyTracker latencyTracker;
    private final OfflineQueue offlineQueue;
    private final boolean enabled;

    /**
     * Private constructor for singleton.
     */
    private CloudflareBridge() {
        this.enabled = MineWrightConfig.CLOUDFLARE_ENABLED.get();

        if (enabled) {
            if (!MineWrightConfig.validateCloudflareConfig()) {
                LOGGER.error("Cloudflare configuration invalid, disabling bridge");
                this.httpClient = null;
                this.stateSynchronizer = null;
                this.latencyTracker = null;
                this.offlineQueue = null;
                return;
            }

            CloudflareConfig config = CloudflareConfig.fromForgeConfig();
            this.httpClient = new CloudflareHttpClient(config);
            this.stateSynchronizer = new StateSynchronizer(httpClient);
            this.latencyTracker = new LatencyTracker();
            this.offlineQueue = new OfflineQueue();

            LOGGER.info("CloudflareBridge initialized: {}", config.getWorkerUrl());
        } else {
            this.httpClient = null;
            this.stateSynchronizer = null;
            this.latencyTracker = null;
            this.offlineQueue = null;
            LOGGER.info("CloudflareBridge disabled by configuration");
        }
    }

    /**
     * Returns the singleton instance.
     *
     * @return CloudflareBridge instance
     */
    public static CloudflareBridge getInstance() {
        return INSTANCE;
    }

    /**
     * Checks if the bridge is enabled.
     *
     * @return true if enabled and configured
     */
    public boolean isEnabled() {
        return enabled && httpClient != null;
    }

    /**
     * Attempts to get a tactical decision from Cloudflare.
     *
     * <p>This method is non-blocking and returns immediately with an Optional.
     * If Cloudflare is unavailable or times out, returns Optional.empty().</p>
     *
     * <p><b>Usage Pattern:</b></p>
     * <pre>
     * Optional&lt;TacticalResponse&gt; response = bridge.tryTactical(request, Duration.ofMillis(20));
     * if (response.isPresent()) {
     *     // Apply optimization
     * } else {
     *     // Fall back to local logic
     * }
     * </pre>
     *
     * @param request Tactical request
     * @param timeout Maximum time to wait
     * @return Optional with response, or empty if unavailable/timeout
     */
    public Optional<TacticalResponse> tryTactical(TacticalRequest request, Duration timeout) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        long startTime = System.currentTimeMillis();

        try {
            CompletableFuture<TacticalResponse> future = httpClient.sendTacticalAsync(request);

            // Block with timeout
            TacticalResponse response = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (response != null) {
                long latency = System.currentTimeMillis() - startTime;
                latencyTracker.recordTactical(latency, false);

                LOGGER.debug("Tactical response received in {}ms", latency);
                return Optional.of(response);
            }
        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.debug("Tactical request timed out after {}ms", timeout.toMillis());
            latencyTracker.recordTactical(timeout.toMillis(), true);
        } catch (Exception e) {
            LOGGER.debug("Tactical request failed: {}", e.getMessage());
            latencyTracker.recordTactical(System.currentTimeMillis() - startTime, true);
        }

        return Optional.empty();
    }

    /**
     * Sends a strategic request asynchronously.
     *
     * @param request Strategic request
     * @return CompletableFuture with response
     */
    public CompletableFuture<StrategicResponse> sendStrategicAsync(StrategicRequest request) {
        if (!isEnabled()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Cloudflare bridge is disabled")
            );
        }

        return httpClient.sendStrategicAsync(request);
    }

    /**
     * Syncs agent state to Cloudflare (asynchronous, fire-and-forget).
     *
     * @param agentId Agent ID
     * @param state State to sync
     */
    public void syncStateAsync(String agentId, AgentState state) {
        if (!isEnabled()) {
            return;
        }

        stateSynchronizer.recordStateChange(agentId, state);
    }

    /**
     * Sends telemetry asynchronously (fire-and-forget).
     *
     * @param event Telemetry event
     */
    public void sendTelemetryAsync(TelemetryEvent event) {
        if (!isEnabled() || !MineWrightConfig.CLOUDFLARE_TELEMETRY_ENABLED.get()) {
            return;
        }

        httpClient.sendTelemetryAsync(event);
    }

    /**
     * Drains the offline queue (call on reconnection).
     *
     * @return Number of deltas sent
     */
    public int drainOfflineQueue() {
        if (!isEnabled()) {
            return 0;
        }

        return offlineQueue.drain(httpClient);
    }

    /**
     * Gets the latency tracker for monitoring.
     *
     * @return LatencyTracker instance
     */
    public LatencyTracker getLatencyTracker() {
        return latencyTracker;
    }

    /**
     * Logs current metrics.
     */
    public void logMetrics() {
        if (latencyTracker != null) {
            latencyTracker.logMetrics();
        }
        if (offlineQueue != null) {
            LOGGER.info("Offline queue size: {}", offlineQueue.size());
        }
    }
}
```

### 7.2 TacticalRequest Record

```java
package com.minewright.cloudflare.model;

import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.ActionContext;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Request for tactical decision from Cloudflare Worker.
 *
 * <p>Tactical requests are for real-time decisions that need to happen
 * within a single Minecraft tick (50ms):</p>
 * <ul>
 *   <li>Combat targeting and positioning</li>
 *   <li>Pathfinding optimization</li>
 *   <li>Block placement decisions</li>
 *   <li>Evasive maneuvers</li>
 * </ul>
 */
public record TacticalRequest(
    String agentId,
    String actionType,
    Map<String, Object> context,
    Position agentPosition,
    Position targetPosition,
    long timestamp
) {
    /**
     * Creates a tactical request from an action.
     *
     * @param action Action to request tactical optimization for
     * @param context Action context
     * @return TacticalRequest
     */
    public static TacticalRequest fromAction(BaseAction action, ActionContext context) {
        ForemanEntity foreman = action.foreman;

        return new TacticalRequest(
            foreman.getSteveName(),
            action.getClass().getSimpleName(),
            buildContext(action, context, foreman),
            Position.fromEntity(foreman),
            extractTargetPosition(action),
            System.currentTimeMillis()
        );
    }

    private static Map<String, Object> buildContext(
            BaseAction action,
            ActionContext context,
            ForemanEntity foreman) {

        Map<String, Object> ctx = new HashMap<>();

        // Agent state
        ctx.put("state", context.getCurrentState().name());
        ctx.put("health", foreman.getHealth());
        ctx.put("maxHealth", foreman.getMaxHealth());

        // Action-specific context
        if (action instanceof CombatAction) {
            ctx.put("target", ((CombatAction) action).getTargetEntity().getUUID());
            ctx.put("weapon", ((CombatAction) action).getWeaponType());
        } else if (action instanceof PathfindAction) {
            ctx.put("destination", ((PathfindAction) action).getDestination().toString());
        }

        return ctx;
    }

    private static Position extractTargetPosition(BaseAction action) {
        if (action instanceof PathfindAction) {
            return Position.fromBlockPos(((PathfindAction) action).getDestination());
        }
        if (action instanceof CombatAction) {
            return Position.fromEntity(((CombatAction) action).getTargetEntity());
        }
        return null;
    }
}

/**
 * Position in the world.
 */
public record Position(int x, int y, int z) {
    public static Position fromEntity(Entity entity) {
        BlockPos pos = entity.blockPosition();
        return new Position(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Position fromBlockPos(BlockPos pos) {
        return new Position(pos.getX(), pos.getY(), pos.getZ());
    }

    public double distanceTo(Position other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
```

### 7.3 Integration in BaseAction.tick()

```java
package com.minewright.action.actions;

import com.minewright.cloudflare.CloudflareBridge;
import com.minewright.cloudflare.model.TacticalRequest;
import com.minewright.execution.ActionContext;

/**
 * Base action with Cloudflare integration.
 */
public abstract class BaseAction {
    // ... existing fields

    /**
     * Called every tick. Override to implement action logic.
     */
    protected void onTick() {
        // Optional: Consult Cloudflare for tactical optimization
        if (this instanceof TacticalAction) {
            consultCloudflare();
        }

        // Default tick implementation
        // ... existing tick logic
    }

    /**
     * Attempts to get tactical optimization from Cloudflare.
     *
     * <p>This is non-blocking and falls back to local logic if
     * Cloudflare is unavailable.</p>
     */
    protected void consultCloudflare() {
        CloudflareBridge bridge = CloudflareBridge.getInstance();
        if (!bridge.isEnabled()) {
            return;
        }

        // Create tactical request
        // Note: We need access to actionContext here
        // This could be passed in constructor or stored in BaseAction
        TacticalRequest request = TacticalRequest.fromAction(this, getActionContext());

        // Try to get response with 20ms timeout
        var response = bridge.tryTactical(request, Duration.ofMillis(20));

        if (response.isPresent()) {
            // Apply optimization
            applyTacticalOptimization(response.get());
        }
        // Else: Proceed with local logic (implicit fallback)
    }

    /**
     * Applies tactical optimization from Cloudflare.
     *
     * @param response Tactical response
     */
    protected void applyTacticalOptimization(TacticalResponse response) {
        if (!response.success()) {
            return;
        }

        String hint = response.optimizationHint();
        Map<String, Object> params = response.parameters();

        switch (hint) {
            case "ADJUST_PATH" -> adjustPath(params);
            case "CHANGE_TARGET" -> changeTarget(params);
            case "EVADE" -> evade(params);
            case "OPTIMAL_PLACEMENT" -> optimalBlockPlacement(params);
            // ... more hints
        }
    }

    // Action-specific optimization methods
    protected void adjustPath(Map<String, Object> params) {
        // Override in subclasses
    }

    protected void changeTarget(Map<String, Object> params) {
        // Override in subclasses
    }

    protected void evade(Map<String, Object> params) {
        // Override in subclasses
    }

    protected void optimalBlockPlacement(Map<String, Object> params) {
        // Override in subclasses
    }
}

/**
 * Marker interface for actions that support tactical optimization.
 */
public interface TacticalAction {
    /**
     * Gets the action context for Cloudflare requests.
     */
    ActionContext getActionContext();
}
```

### 7.4 Error Handling and Fallbacks

```java
package com.minewright.cloudflare;

import com.minewright.config.MineWrightConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Fallback handler for when Cloudflare is unavailable.
 */
public class CloudflareFallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudflareFallback.class);

    /**
     * Executes a request with local fallback.
     *
     * @param cloudflareRequest Request to Cloudflare
     * @param localFallback Local fallback logic
     * @param <T> Response type
     * @return Response from Cloudflare or local fallback
     */
    public static <T> T executeWithFallback(
            Supplier<Optional<T>> cloudflareRequest,
            Supplier<T> localFallback) {

        // Try Cloudflare first
        try {
            Optional<T> response = cloudflareRequest.get();
            if (response.isPresent()) {
                LOGGER.debug("Using Cloudflare response");
                return response.get();
            }
        } catch (Exception e) {
            LOGGER.debug("Cloudflare request failed: {}", e.getMessage());
        }

        // Fall back to local
        if (MineWrightConfig.CLOUDFLARE_OFFLINE_FALLBACK.get()) {
            LOGGER.debug("Using local fallback");
            return localFallback.get();
        } else {
            LOGGER.warn("Cloudflare unavailable and offline fallback disabled");
            throw new IllegalStateException("No AI processing available");
        }
    }
}

/**
 * Circuit breaker for Cloudflare requests.
 */
public class CloudflareCircuitBreaker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudflareCircuitBreaker.class);

    private static final int FAILURE_THRESHOLD = 5;
    private static final long OPEN_TIMEOUT_MS = 30000; // 30 seconds

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private volatile boolean isOpen = false;

    /**
     * Checks if the circuit is open (requests being rejected).
     *
     * @return true if circuit is open
     */
    public boolean isOpen() {
        if (!isOpen) {
            return false;
        }

        // Check if we should attempt to close the circuit
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
        if (timeSinceLastFailure > OPEN_TIMEOUT_MS) {
            LOGGER.info("Attempting to close circuit breaker");
            isOpen = false;
            failureCount.set(0);
            return false;
        }

        return true;
    }

    /**
     * Records a successful request.
     */
    public void recordSuccess() {
        failureCount.set(0);
        isOpen = false;
    }

    /**
     * Records a failed request.
     */
    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());

        if (failures >= FAILURE_THRESHOLD && !isOpen) {
            LOGGER.warn("Circuit breaker opened after {} consecutive failures", failures);
            isOpen = true;
        }
    }
}
```

---

## 8. Security Considerations

### 8.1 API Token Management

```java
package com.minewright.cloudflare.security;

import com.minewright.config.MineWrightConfig;

/**
 * Secure storage and usage of API tokens.
 */
public class ApiTokenManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiTokenManager.class);
    private static final int MIN_TOKEN_LENGTH = 32;

    /**
     * Gets the API token with validation.
     *
     * @return API token, or null if invalid
     */
    public static String getValidatedToken() {
        String token = MineWrightConfig.CLOUDFLARE_API_TOKEN.get();

        if (token == null || token.trim().isEmpty()) {
            LOGGER.error("API token is not configured");
            return null;
        }

        if (token.length() < MIN_TOKEN_LENGTH) {
            LOGGER.error("API token is too short (expected >= {} chars)", MIN_TOKEN_LENGTH);
            return null;
        }

        // Token looks valid, log preview
        String preview = token.substring(0, 4) + "..." + token.substring(token.length() - 4);
        LOGGER.info("Using API token: {}", preview);

        return token;
    }

    /**
     * Checks if the API token is configured and valid.
     *
     * @return true if valid
     */
    public static boolean isTokenValid() {
        return getValidatedToken() != null;
    }
}
```

### 8.2 Request Signing

```java
package com.minewright.cloudflare.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Signs requests for Cloudflare Worker authentication.
 *
 * <p><b>Algorithm:</b> HMAC-SHA256</p>
 *
 * <p><b>Format:</b></p>
 * <pre>
 * Signature = Base64(HMAC-SHA256(apiToken, requestBody))
 * Header: X-MineWright-Signature: {signature}
 * </pre>
 */
public class RequestSigner {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Signs a request body.
     *
     * @param requestBody Request body as JSON string
     * @param apiToken API token for signing
     * @return Base64-encoded signature
     */
    public static String sign(String requestBody, String apiToken) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(
                apiToken.getBytes(StandardCharsets.UTF_8),
                ALGORITHM
            );
            mac.init(key);

            byte[] signature = mac.doFinal(
                requestBody.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to sign request", e);
        }
    }

    /**
     * Adds signature headers to an HTTP request.
     *
     * @param builder HTTP request builder
     * @param requestBody Request body
     * @param apiToken API token
     */
    public static void signRequest(
            HttpRequest.Builder builder,
            String requestBody,
            String apiToken) {

        String signature = sign(requestBody, apiToken);
        String timestamp = String.valueOf(System.currentTimeMillis());

        builder.header("X-MineWright-Signature", signature);
        builder.header("X-MineWright-Timestamp", timestamp);
    }
}
```

### 8.3 HTTPS Only

```java
/**
 * Cloudflare HTTP client with HTTPS enforcement.
 */
public class CloudflareHttpClient {

    /**
     * Validates that the URL uses HTTPS.
     *
     * @param url URL to validate
     * @throws IllegalArgumentException if not HTTPS
     */
    private static void requireHttps(String url) {
        if (!url.startsWith("https://")) {
            throw new IllegalArgumentException(
                "Cloudflare Worker URL must use HTTPS. Got: " + url
            );
        }
    }

    /**
     * Creates an HTTP client with security settings.
     *
     * @return Configured HttpClient
     */
    private static HttpClient createSecureClient() {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .sslContext(/* Use system default */)
            .build();
    }
}
```

### 8.4 Rate Limiting

```java
package com.minewright.cloudflare.security;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter for Cloudflare requests.
 *
 * <p>Prevents accidental API abuse and respects rate limits.</p>
 */
public class RateLimiter {

    private final Semaphore semaphore;
    private final int maxRequestsPerSecond;

    /**
     * Creates a rate limiter.
     *
     * @param maxRequestsPerSecond Maximum requests per second
     */
    public RateLimiter(int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
        this.semaphore = new Semaphore(maxRequestsPerSecond);
    }

    /**
     * Acquires a permit for a request.
     *
     * @return true if permit acquired, false if rate limited
     */
    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    /**
     * Acquires a permit, blocking if necessary.
     *
     * @param timeoutMs Maximum time to wait
     * @return true if permit acquired
     */
    public boolean tryAcquire(long timeoutMs) {
        try {
            return semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Releases a permit (call after request completes).
     */
    public void release() {
        semaphore.release();
    }
}
```

---

## 9. Implementation Checklist

- [ ] Create `com.minewright.cloudflare` package structure
- [ ] Implement `CloudflareBridge` singleton
- [ ] Implement `CloudflareHttpClient` with Java 11 HttpClient
- [ ] Add configuration to `MineWrightConfig.java`
- [ ] Implement request/response models (TacticalRequest, StrategicResponse, etc.)
- [ ] Implement `StateSynchronizer` with offline queue
- [ ] Add `CloudflareTacticalInterceptor` to interceptor chain
- [ ] Integrate in `ActionExecutor.tick()` and `AgentStateMachine.transitionTo()`
- [ ] Implement `LatencyTracker` for monitoring
- [ ] Add circuit breaker and retry logic
- [ ] Implement security features (request signing, HTTPS enforcement, rate limiting)
- [ ] Write unit tests for all components
- [ ] Write integration tests with mock Cloudflare Worker
- [ ] Update documentation and examples
- [ ] Test offline fallback behavior
- [ ] Test latency under load (ensure <20ms for tactical)

---

## 10. Appendix

### 10.1 Cloudflare Worker Template

```javascript
// Cloudflare Worker for MineWright tactical decisions
export default {
  async fetch(request, env) {
    try {
      const url = new URL(request.url);
      const path = url.pathname;

      // Verify authentication
      const authHeader = request.headers.get('Authorization');
      if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return new Response('Unauthorized', { status: 401 });
      }

      const token = authHeader.substring(7);
      if (token !== env.API_TOKEN) {
        return new Response('Invalid token', { status: 403 });
      }

      // Route request
      if (path === '/tactical') {
        return handleTactical(await request.json());
      } else if (path === '/strategic') {
        return handleStrategic(await request.json());
      } else if (path === '/sync/delta') {
        return handleSyncDelta(await request.json());
      } else if (path === '/telemetry') {
        return handleTelemetry(await request.json());
      }

      return new Response('Not found', { status: 404 });

    } catch (error) {
      return new Response(error.message, { status: 500 });
    }
  }
};

async function handleTactical(request) {
  const startTime = Date.now();

  // Process tactical decision
  const response = {
    success: true,
    optimizationHint: determineOptimization(request),
    parameters: calculateParameters(request),
    processingTimeMs: Date.now() - startTime
  };

  return new Response(JSON.stringify(response), {
    headers: { 'Content-Type': 'application/json' }
  });
}

function determineOptimization(request) {
  // Implement tactical logic here
  switch (request.actionType) {
    case 'CombatAction':
      return 'CHANGE_TARGET';
    case 'PathfindAction':
      return 'ADJUST_PATH';
    default:
      return null;
  }
}

function calculateParameters(request) {
  // Calculate optimization parameters
  return {};
}
```

### 10.2 Testing Strategy

```java
@Test
public void testTacticalRequestTimeout() {
    // Given: Cloudflare is slow
    when(httpClient.sendTacticalAsync(any()))
        .thenReturn CompletableFuture
            .completedFuture(response)
            .completeOnTimeout(null, 100, TimeUnit.MILLISECONDS);

    // When: Request with 20ms timeout
    Optional<TacticalResponse> result = bridge.tryTactical(
        request,
        Duration.ofMillis(20)
    );

    // Then: Should return empty (fallback)
    assertTrue(result.isEmpty());
    verify(latencyTracker).recordTactical(anyInt(), eq(true));
}

@Test
public void testOfflineFallback() {
    // Given: Cloudflare is disabled
    when(config.enabled()).thenReturn(false);

    // When: Try tactical request
    Optional<TacticalResponse> result = bridge.tryTactical(request, timeout);

    // Then: Should return empty immediately
    assertTrue(result.isEmpty());
}

@Test
public void testStateSyncWithOfflineQueue() {
    // Given: Cloudflare is unavailable
    when(httpClient.sendAsync(any(), any(), any()))
        .thenReturn(CompletableFuture.failedFuture(new IOException()));

    // When: Sync state
    bridge.syncStateAsync("agent1", AgentState.EXECUTING);

    // Then: Should be queued
    assertEquals(1, offlineQueue.size());
}
```

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Next Review:** After implementation phase
