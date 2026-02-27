# Error Recovery and Resilience Patterns for AI Agents

**Research Date:** 2026-02-27
**Focus Area:** Error recovery, resilience patterns, and graceful degradation for AI agent systems
**Target System:** MineWright (MineWright AI - Minecraft Mod)

---

## Executive Summary

This document compiles research findings on error recovery and resilience patterns specifically for AI agent systems, with direct applications to the MineWright codebase. The research covers classification of error types, automatic retry strategies, escalation mechanisms, fallback action strategies, user notification patterns, and recovery from partial completion.

### Key Findings

1. **Error Classification**: AI agents face 10 distinct failure modes requiring different recovery strategies
2. **Recovery Hierarchy**: Multi-level fallback systems (cache → secondary provider → pattern-based → safe default) achieve 90%+ success rates
3. **Circuit Breaker Pattern**: Essential for preventing cascading failures in LLM provider chains
4. **Exponential Backoff with Jitter**: Gold standard for retry strategies, preventing "thundering herd" problems
5. **State Machine Recovery**: Critical for preventing agents from getting stuck in error states

---

## Table of Contents

1. [Error Classification Framework](#error-classification-framework)
2. [Retry Strategies and Patterns](#retry-strategies-and-patterns)
3. [Circuit Breaker Implementation](#circuit-breaker-implementation)
4. [Graceful Degradation Levels](#graceful-degradation-levels)
5. [Fallback Action Strategies](#fallback-action-strategies)
6. [User Notification Patterns](#user-notification-patterns)
7. [Partial Completion Recovery](#partial-completion-recovery)
8. [Multi-Agent Coordination Failures](#multi-agent-coordination-failures)
9. [Improvements for MineWright](#improvements-for-minewright)
10. [Implementation Roadmap](#implementation-roadmap)

---

## 1. Error Classification Framework

### 1.1 The 10 Common AI Agent Failure Modes (2025)

Based on industry research from Google, Anthropic, and enterprise AI deployments:

| # | Failure Mode | Description | Recovery Strategy | Retryable |
|---|--------------|-------------|-------------------|-----------|
| 1 | **Hallucination** | Agent fabricates facts/steps that don't exist | Validation, factual verification | No |
| 2 | **Tool Misuse** | Incorrect tool usage or unclear tool descriptions | Better tool definitions, examples | Yes |
| 3 | **Infinite Loops** | Agent stuck in planning or retry cycles | Timeout with escalation | No |
| 4 | **Brittle Plans** | Linear reasoning without reassessment | Checkpoint-based replanning | Yes |
| 5 | **Over-delegation** | Confusion in multi-agent role assignment | Clear role definitions | Yes |
| 6 | **Cascade Errors** | Missing checkpoints between steps | Add validation gates | No |
| 7 | **Context Overflow** | Exceeding context window limits | Context compression, summarization | Yes |
| 8 | **Unsafe Behaviors** | Security-related failures | Safety filters, human-in-loop | No |
| 9 | **Tool Unavailability** | Required tool/service is down | Fallback tools or graceful degradation | Yes |
| 10 | **Ambiguous Commands** | User intent unclear | Clarification prompts | No |

### 1.2 LLM API Error Types (Current Implementation)

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\LLMException.java`

The codebase already implements a solid error type classification:

```java
public enum ErrorType {
    RATE_LIMIT(true),        // HTTP 429 - Retryable
    TIMEOUT(true),           // Request timeout - Retryable
    CIRCUIT_OPEN(false),     // Circuit breaker open - Not retryable
    INVALID_RESPONSE(false), // Malformed JSON - Not retryable
    NETWORK_ERROR(true),     // Connection issues - Retryable
    AUTH_ERROR(false),       // Invalid API key - Not retryable
    SERVER_ERROR(true),      // HTTP 5xx - Retryable
    CLIENT_ERROR(false);     // HTTP 4xx (except 429) - Not retryable
}
```

### 1.3 Action Execution Error Types

**File:** `C:\Users\casey\steve\docs\ACTION_RETRY_RECOVERY.md`

Minecraft-specific failure modes requiring specialized handling:

| Error Type | Description | Current Handling | Recommended Improvement |
|------------|-------------|------------------|------------------------|
| **Pathfinding Blockage** | Navigation blocked by entities/blocks | Timeout after 30s | Retry with alternate routes (3x) |
| **Mob Interference** | Hostile mobs interrupting | None | Clear mobs, retry with delay |
| **Block Changes** | Target block changed during action | None | Re-scan, update target, retry |
| **Movement Stuck** | Entity stuck on terrain | Timeout | Teleport unstuck, retry |
| **Resource Depletion** | Materials ran out mid-task | None | Request materials, pause task |
| **Unreachable Location** | Target in unloaded chunk/void | None | Mark unreachable, skip with message |
| **Permission Denied** | Spawn protection | None | Detect early, suggest alternative |
| **Chunk Unload** | Entity in unloaded chunk | None | Teleport to player, fail gracefully |

---

## 2. Retry Strategies and Patterns

### 2.1 Exponential Backoff with Jitter (Recommended Standard)

**Why Jitter Matters:**
Without jitter, multiple clients retrying simultaneously create "retry storms" that can overwhelm recovering services.

**Implementation Pattern:**
```java
// Formula: delay = base_delay * (2 ^ attempt) + random_jitter
// Jitter: +/- 20-50% of base delay to distribute retries

private long calculateBackoffDelay(int attempt) {
    long baseDelay = (long) (Math.pow(2, attempt) * 1000); // 1s, 2s, 4s, 8s...
    long jitterRange = (long) (baseDelay * 0.5); // +/- 50% jitter
    long jitter = (long) (Math.random() * jitterRange) - (jitterRange / 2);
    return baseDelay + jitter;
}
```

**Current Implementation Status:**
- **OpenAIClient.java (legacy)**: Implements exponential backoff WITHOUT jitter
- **AsyncOpenAIClient.java**: No backoff implementation (defers to Resilience4j)
- **ResilienceConfig.java**: Resilience4j retry with exponential backoff (jitter not explicitly configured)

**Recommendation:** Add jitter to all retry implementations to prevent retry storms.

### 2.2 Retry Strategy Selection Guide

| Scenario | Strategy | Max Attempts | Base Delay | Reason |
|----------|----------|--------------|------------|--------|
| **Rate Limiting (429)** | Exponential + Jitter | 5 | 2s | Respect rate limits, spread retries |
| **Timeout** | Exponential + Jitter | 3 | 1s | May be temporary congestion |
| **Network Error** | Exponential + Jitter | 4 | 1s | Connection may recover |
| **Server Error (5xx)** | Exponential + Jitter | 3 | 2s | Server may recover quickly |
| **Block Placement** | Fixed Delay | 5 | 40 ticks (2s) | Need cooldown between attempts |
| **Pathfinding** | Exponential + Jitter | 3 | 1s | Alternate routes may help |
| **Mining** | Immediate | 2 | 0 | Fail fast for mining |

### 2.3 Truncated Exponential Backoff

Prevent infinite waits by capping maximum delay:

```java
public class TruncatedExponentialBackoff {
    private static final long INITIAL_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 60000; // 60 second cap
    private static final double MULTIPLIER = 2.0;

    public static long calculateDelay(int attempt) {
        long delay = (long) (INITIAL_DELAY_MS * Math.pow(MULTIPLIER, attempt));
        return Math.min(delay, MAX_DELAY_MS);
    }
}
```

### 2.4 Current MineWright Retry Implementation Analysis

**Strengths:**
- Comprehensive error type classification in `LLMException`
- Resilience4j integration configured in `ResilienceConfig`
- Event listeners for retry observability

**Weaknesses:**
1. **Resilience4j circuit breaker unused** due to Forge classloading issues
2. **No jitter** in retry delays (potential retry storms)
3. **AsyncGroqClient and AsyncGeminiClient** lack retry logic
4. **TaskPlanner returns null** on errors instead of using Result types
5. **ActionExecutor uses blocking `planningFuture.get()`** in tick loop

**Current Code (OpenAIClient.java, lines 46-68):**
```java
for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
    try {
        HttpResponse<String> response = client.send(request, ...);
        if (response.statusCode() == 200) {
            return parseResponse(response.body());
        }

        if (response.statusCode() == 429 || response.statusCode() >= 500) {
            if (attempt < MAX_RETRIES - 1) {
                int delayMs = INITIAL_RETRY_DELAY_MS * (int) Math.pow(2, attempt);
                // Missing jitter here!
                Thread.sleep(delayMs);
                continue;
            }
        }
        // ...
    } catch (Exception e) {
        // Similar retry logic without jitter
    }
}
```

---

## 3. Circuit Breaker Implementation

### 3.1 Circuit Breaker Pattern Overview

The Circuit Breaker pattern prevents cascading failures by failing fast when a dependency is experiencing problems.

**Three States:**
1. **CLOSED**: Normal operation, requests pass through
2. **OPEN**: Failure threshold exceeded, requests fail immediately
3. **HALF_OPEN**: Probe requests allowed to test if service has recovered

**State Transitions:**
```
CLOSED ──[failure threshold]──> OPEN ──[timeout]──> HALF_OPEN
  ^                                    │
  │                                    └──[success]──> CLOSED
  └────────────────[success]───────────┘
```

### 3.2 Current Resilience4j Configuration

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\ResilienceConfig.java`

```java
// Circuit Breaker Configuration
- Sliding window size: 10 calls
- Failure rate threshold: 50%
- Wait duration in open state: 30 seconds
- Half-open max calls: 3
```

**Status:** Configured but **NOT ACTIVELY USED** due to Forge classloading issues.

### 3.3 Recommended: Manual Circuit Breaker

Since Resilience4j has classloading issues with Forge, implement a lightweight manual circuit breaker:

```java
public class SimpleCircuitBreaker {
    private enum State { CLOSED, OPEN, HALF_OPEN }

    private volatile State state = State.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final int failureThreshold = 5;
    private final long waitTimeMs = 30000; // 30 seconds
    private volatile long lastFailureTime;
    private final int halfOpenMaxCalls = 3;
    private final AtomicInteger halfOpenCalls = new AtomicInteger(0);

    public boolean allowRequest() {
        synchronized (this) {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime > waitTimeMs) {
                    state = State.HALF_OPEN;
                    halfOpenCalls.set(0);
                    LOGGER.info("Circuit breaker transitioning to HALF_OPEN");
                    return true;
                }
                return false;
            }
            return true;
        }
    }

    public void recordSuccess() {
        synchronized (this) {
            if (state == State.HALF_OPEN) {
                int calls = halfOpenCalls.incrementAndGet();
                if (calls >= halfOpenMaxCalls) {
                    state = State.CLOSED;
                    failureCount.set(0);
                    LOGGER.info("Circuit breaker closing after successful probe");
                }
            } else {
                failureCount.set(0);
            }
        }
    }

    public void recordFailure() {
        synchronized (this) {
            int failures = failureCount.incrementAndGet();
            lastFailureTime = System.currentTimeMillis();

            if (state == State.HALF_OPEN || failures >= failureThreshold) {
                state = State.OPEN;
                LOGGER.warn("Circuit breaker opening after {} failures", failures);
            }
        }
    }

    public State getState() {
        return state;
    }

    public double getFailureRate() {
        int total = failureCount.get() + successCount.get();
        return total == 0 ? 0 : (double) failureCount.get() / total;
    }
}
```

### 3.4 Integration with AsyncOpenAIClient

```java
public class AsyncOpenAIClient implements AsyncLLMClient {
    private final SimpleCircuitBreaker circuitBreaker = new SimpleCircuitBreaker();

    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        // Check circuit breaker first (fast fail)
        if (!circuitBreaker.allowRequest()) {
            return CompletableFuture.failedFuture(
                new LLMException("Circuit breaker is OPEN",
                    LLMException.ErrorType.CIRCUIT_OPEN,
                    PROVIDER_ID,
                    false)
            );
        }

        // Execute request with retry
        return sendWithRetry(request, startTime, 0)
            .whenComplete((response, error) -> {
                if (error != null) {
                    circuitBreaker.recordFailure();
                } else {
                    circuitBreaker.recordSuccess();
                }
            });
    }
}
```

---

## 4. Graceful Degradation Levels

### 4.1 Multi-Level Degradation Strategy

Implement a hierarchy of fallbacks to maintain functionality under increasing failure conditions:

```
Level 1: Cache Hit (Fastest)
    ↓ 40-60% hit rate
Level 2: Secondary Provider
    ↓ 95% availability
Level 3: Pattern-Based Fallback
    ↓ Instant response
Level 4: Safe Default Action
    ↓ Wait/Idle
Level 5: User Notification
    ↓ Manual intervention
```

### 4.2 Level 1: Cache First Strategy

**Current Implementation:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\LLMCache.java`

**Cache Performance:**
- 40-60% hit rate for repetitive commands
- 0 latency, 0 API cost
- LRU eviction policy with 1000 entry max

**Improvement Needed:** Fix race condition in `accessOrder` modification (see AUDIT_ERROR_HANDLING.md, section 3.4).

### 4.3 Level 2: Multi-Provider Fallback

**Recommendation:** Implement provider cascade for high availability:

```java
public class MultiProviderLLMClient implements AsyncLLMClient {
    private final List<AsyncLLMClient> providers;
    private final AtomicInteger currentProviderIndex = new AtomicInteger(0);

    public MultiProviderLLMClient(List<AsyncLLMClient> providers) {
        this.providers = providers;
    }

    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        // Try current provider
        int startIndex = currentProviderIndex.get();
        return tryProvider(startIndex, prompt, params);
    }

    private CompletableFuture<LLMResponse> tryProvider(
            int index, String prompt, Map<String, Object> params) {

        if (index >= providers.size()) {
            // All providers failed
            return CompletableFuture.failedFuture(
                new LLMException("All LLM providers failed",
                    LLMException.ErrorType.SERVER_ERROR,
                    "multi",
                    false)
            );
        }

        AsyncLLMClient provider = providers.get(index);

        return provider.sendAsync(prompt, params)
            .exceptionallyCompose(error -> {
                LOGGER.warn("Provider {} failed: {}",
                    provider.getProviderId(), error.getMessage());

                // Try next provider
                return tryProvider(index + 1, prompt, params);
            });
    }
}
```

### 4.4 Level 3: Pattern-Based Fallback

**Current Implementation:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\LLMFallbackHandler.java`

**Supported Patterns:**
- Mining: "mine", "dig", "collect", "gather"
- Building: "build", "construct", "create"
- Combat: "attack", "fight", "kill"
- Follow: "follow", "come", "with me"
- Movement: "go to", "move to", "walk"
- Placement: "place", "put", "set"
- Stop: "stop", "halt", "cancel", "wait"

**Improvement Recommendations:**

1. **Context-Aware Fallbacks:**
```java
public LLMResponse generateFallback(String prompt, Throwable error, ForemanEntity foreman) {
    // Get world context
    BlockState nearbyBlock = getNearestBlock(foreman);
    EntityType nearbyEntity = getNearestHostile(foreman);

    if (prompt.toLowerCase().contains("mine")) {
        if (nearbyBlock != null) {
            return createMineResponse(nearbyBlock.getBlock().getRegistryName());
        }
        return createExploreResponse(); // No resources nearby
    }

    if (prompt.toLowerCase().contains("attack")) {
        if (nearbyEntity != null) {
            return createAttackResponse(nearbyEntity.getEncodeId());
        }
        return createPatrolResponse(); // No enemies nearby
    }

    return createDefaultResponse();
}
```

2. **Cached Fallback Responses:**
```java
public class CachedFallbackHandler {
    private final LoadingCache<String, LLMResponse> fallbackCache;

    public CachedFallbackHandler() {
        this.fallbackCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(key -> generateFallbackUncached(key));
    }
}
```

### 4.5 Level 4: Safe Default Actions

When all else fails, return safe passive actions:

```java
private LLMResponse createSafeDefault(String prompt) {
    return LLMResponse.builder()
        .content("{\"thoughts\":\"[Fallback] Waiting for instructions\",\"tasks\":[{\"action\":\"wait\",\"duration\":5}]}")
        .providerId("safe-default")
        .model("fallback")
        .latencyMs(0)
        .tokensUsed(0)
        .fromCache(false)
        .build();
}
```

### 4.6 Level 5: User Notification and Escalation

When automatic recovery fails, notify the user:

```java
public void notifyUserOfFailure(ForemanEntity foreman, String command, Throwable error) {
    NotificationLevel level = classifySeverity(error);

    switch (level) {
        case INFO ->
            sendToGUI(foreman, "I'm having trouble with that command, trying a fallback...");
        case WARNING ->
            sendToChat(foreman, "The AI service is slow. Using offline mode.");
        case ERROR -> {
            sendToChat(foreman, "I couldn't process: \"" + command + "\"");
            sendToChat(foreman, "Reason: " + error.getMessage());
            suggestAlternatives(foreman, command);
        }
        case CRITICAL -> {
            sendTitle(foreman, "AI System Unavailable", "Please try again later");
            playSound(foreman, SoundEvents.NOTE_BLOCK_DIDGERIDOO);
        }
    }
}
```

---

## 5. Fallback Action Strategies

### 5.1 Recovery Chain Pattern

Implement a chain of recovery strategies from least to most invasive:

```java
public class RecoveryAction extends BaseAction {
    private final BaseAction originalAction;
    private final List<Supplier<BaseAction>> recoveryStrategies;
    private int currentStrategy = 0;

    public RecoveryAction(BaseAction original, List<Supplier<BaseAction>> strategies) {
        super(original.foreman, original.task);
        this.originalAction = original;
        this.recoveryStrategies = strategies;
    }

    @Override
    protected void onTick() {
        BaseAction current = getCurrentAction();
        current.tick();

        if (current.isComplete()) {
            ActionResult result = current.getResult();

            if (result.isSuccess()) {
                this.result = result; // Success!
            } else if (currentStrategy < recoveryStrategies.size()) {
                // Try next strategy
                currentStrategy++;
                tryCurrentStrategy();
            } else {
                // All strategies failed
                this.result = ActionResult.failure("All recovery strategies failed");
            }
        }
    }
}
```

### 5.2 Example: Place Block Recovery Chain

```java
List<Supplier<BaseAction>> recoveryChain = new ArrayList<>();

// Strategy 1: Normal placement (original)
recoveryChain.add(() -> new PlaceBlockAction(foreman, task));

// Strategy 2: Clear obstruction and retry
recoveryChain.add(() -> {
    BlockPos targetPos = getTargetPosition();
    foreman.level().destroyBlock(targetPos, true);
    return new PlaceBlockAction(foreman, task);
});

// Strategy 3: Pathfind closer and retry
recoveryChain.add(() -> new CompositeAction(foreman, task,
    new PathfindAction(foreman, createPathfindTask()),
    new PlaceBlockAction(foreman, task)
));

// Strategy 4: Request player assistance
recoveryChain.add(() -> {
    foreman.sendChatMessage("I need help placing a block at " + getTargetPosition());
    return new WaitForPlayerAction(foreman, task);
});

RecoveryAction resilientAction = new RecoveryAction(
    new PlaceBlockAction(foreman, task),
    recoveryChain
);
```

### 5.3 Partial Progress Preservation

Save state to avoid restarting from scratch:

```java
public abstract class StatefulAction extends BaseAction {
    protected ActionState savedState;

    protected void saveState() {
        savedState = new ActionState();
        savedState.timestamp = System.currentTimeMillis();
        savedState.progress = calculateProgress();
        savedState.partialResults = getPartialResults();
    }

    protected void restoreState(ActionState state) {
        this.savedState = state;
        applyPartialResults(state.partialResults);
    }

    @Override
    protected void onCancel() {
        saveState(); // Save for potential recovery
    }
}
```

**Example: Build Structure with Progress Preservation**

```java
public class ResilientBuildStructureAction extends StatefulAction {
    private Set<BlockPos> placedBlocks;

    @Override
    protected void onStart() {
        placedBlocks = new HashSet<>();

        ActionState saved = loadSavedState();
        if (saved != null) {
            restoreState(saved);
            foreman.sendChatMessage("Continuing build from " + saved.progress + "%");
        }
    }

    @Override
    protected void onTick() {
        BlockPos nextPos = getNextBlockPosition();
        placeBlock(nextPos);
        placedBlocks.add(nextPos);

        // Save progress every 10 blocks
        if (placedBlocks.size() % 10 == 0) {
            saveState();
        }
    }
}
```

---

## 6. User Notification Patterns

### 6.1 Notification Severity Levels

| Level | Trigger | User Action Required | Example |
|-------|---------|---------------------|---------|
| **INFO** | Successful completion | No | "Built house successfully!" |
| **WARNING** | Recoverable failure | Optional | "Path blocked, taking alternative route" |
| **ERROR** | Unrecoverable failure | Yes (usually) | "Can't reach target location" |
| **CRITICAL** | Multiple failures | Yes | "Stuck in error loop, please help" |

### 6.2 Notification Implementation

```java
public class ActionFailureNotifier {
    public static void notifyFailure(ForemanEntity foreman,
                                     BaseAction action,
                                     ActionResult result,
                                     NotificationLevel level) {
        String message = formatFailureMessage(action, result, level);

        // Log the failure
        logFailure(foreman, action, result, level);

        // Send in-game notification
        switch (level) {
            case INFO -> sendToGUI(foreman, message);
            case WARNING -> sendToGUI(foreman, message);
            case ERROR -> sendToChat(foreman, message);
            case CRITICAL -> {
                sendToChat(foreman, message);
                sendTitle(foreman, "Action Failed", message);
                playSound(foreman, SoundEvents.NOTE_BLOCK_DIDGERIDOO);
            }
        }

        // Store in memory for learning
        foreman.getMemory().recordFailure(
            action.getClass().getSimpleName(),
            result.getMessage(),
            level
        );
    }
}
```

### 6.3 Interactive Recovery Prompts

Offer users recovery options when automatic recovery fails:

```java
public class RecoveryPrompt {
    public void showRecoveryOptions(ForemanEntity foreman, BaseAction failedAction) {
        sendTitle(foreman, "Action Failed", failedAction.getResult().getMessage());

        List<RecoveryOption> options = generateRecoveryOptions(failedAction);

        foreman.sendChatMessage("What should I do?");
        for (int i = 0; i < options.size(); i++) {
            foreman.sendChatMessage(String.format("  [%d] %s", i + 1, options.get(i).description));
        }

        storePendingRecovery(foreman, options);
    }

    private List<RecoveryOption> generateRecoveryOptions(BaseAction action) {
        List<RecoveryOption> options = new ArrayList<>();

        options.add(new RecoveryOption("Retry the action", () -> retryAction(action)));
        options.add(new RecoveryOption("Skip this step", () -> skipAction(action)));
        options.add(new RecoveryOption("Wait for your help", () -> waitForHelp(action)));

        // Context-specific options
        if (action instanceof PlaceBlockAction) {
            options.add(new RecoveryOption("Clear area and retry", () -> clearAndRetry(action)));
        } else if (action instanceof PathfindAction) {
            options.add(new RecoveryOption("Teleport to target", () -> teleportToTarget(action)));
        }

        return options;
    }
}
```

---

## 7. Partial Completion Recovery

### 7.1 State Snapshot Pattern

Preserve progress when actions need restart:

```java
public class ActionState implements Serializable {
    public long timestamp;
    public int progress; // 0-100
    public Map<String, Object> partialResults;
    public Map<String, Object> context;

    // Serialization for persistence
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putLong("timestamp", timestamp);
        nbt.putInt("progress", progress);
        // ... serialize maps
        return nbt;
    }

    public static ActionState fromNbt(NbtCompound nbt) {
        ActionState state = new ActionState();
        state.timestamp = nbt.getLong("timestamp");
        state.progress = nbt.getInt("progress");
        // ... deserialize maps
        return state;
    }
}
```

### 7.2 Persistence Manager

```java
public class PersistenceManager {
    private static final Map<String, ActionState> stateCache = new ConcurrentHashMap<>();

    public static void saveActionState(String key, ActionState state) {
        stateCache.put(key, state);

        // Optional: Persist to disk for crash recovery
        // saveToDisk(key, state);
    }

    public static ActionState loadActionState(String key) {
        return stateCache.get(key);
    }

    public static void deleteActionState(String key) {
        stateCache.remove(key);
    }
}
```

### 7.3 Batch Operation Recovery

For operations that process multiple items:

```java
public class BatchOperationAction extends StatefulAction {
    private List<Item> items;
    private Set<Item> completedItems;
    private Item currentItem;

    @Override
    protected void onStart() {
        ActionState saved = loadSavedState();
        if (saved != null) {
            restoreState(saved);
            // Skip completed items
            items.removeAll(completedItems);
            foreman.sendChatMessage("Resuming batch operation: " +
                completedItems.size() + "/" + items.size() + " completed");
        }
    }

    @Override
    protected void onTick() {
        if (items.isEmpty()) {
            result = ActionResult.success("Batch completed: " + completedItems.size());
            return;
        }

        currentItem = items.get(0);
        processItem(currentItem);

        if (isItemComplete(currentItem)) {
            completedItems.add(currentItem);
            items.remove(0);

            // Save progress every 10 items
            if (completedItems.size() % 10 == 0) {
                saveState();
            }
        }
    }
}
```

---

## 8. Multi-Agent Coordination Failures

### 8.1 Collaborative Build Failure Modes

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\CollaborativeBuildManager.java`

**Current Issues (from AUDIT_ERROR_HANDLING.md):**
1. `build.sections` is regular ArrayList, not thread-safe
2. Redundant null check on `sectionIndex`
3. Empty if block does nothing
4. Double-call to `getNextBlock()` can return different blocks

**Recommended Fix:**

```java
public static synchronized BlockPlacement getNextBlock(
        CollaborativeBuild build, String foremanName) {
    if (build.isComplete()) {
        return null;
    }

    build.participatingForemen.add(foremanName);

    // Atomic section assignment
    Integer sectionIndex = build.foremanToSectionMap.computeIfAbsent(
        foremanName,
        k -> assignForemanToSection(build, foremanName)
    );

    if (sectionIndex == null) {
        return null;
    }

    BuildSection section = build.sections.get(sectionIndex);
    return section.getNextBlock();
}
```

### 8.2 Deadlock Prevention

Multiple agents waiting on each other:

```java
public class DeadlockDetector {
    private final Map<String, String> waitingGraph = new ConcurrentHashMap<>();
    private final long deadlockTimeoutMs = 30000; // 30 seconds

    public void recordWait(String agentId, String resourceId) {
        waitingGraph.put(agentId, resourceId);

        // Check for cycles (deadlock)
        if (hasCycle()) {
            LOGGER.error("Deadlock detected: {}", waitingGraph);
            resolveDeadlock();
        }
    }

    private boolean hasCycle() {
        // Detect cycles in waiting graph
        Set<String> visited = new HashSet<>();
        for (String agent : waitingGraph.keySet()) {
            if (dfs(agent, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfs(String agent, Set<String> visited) {
        if (visited.contains(agent)) {
            return true; // Cycle detected
        }
        visited.add(agent);

        String resource = waitingGraph.get(agent);
        if (resource != null && waitingGraph.containsKey(resource)) {
            return dfs(resource, visited);
        }
        return false;
    }

    private void resolveDeadlock() {
        // Force one agent to yield
        waitingGraph.keySet().stream()
            .findFirst()
            .ifPresent(agent -> {
                LOGGER.warn("Forcing agent {} to yield", agent);
                // Notify agent to retry with different strategy
            });
    }
}
```

---

## 9. Improvements for MineWright

### 9.1 Priority 1: Fix Critical Issues

From `AUDIT_ERROR_HANDLING.md`:

#### 1.1 Fix Resource Leaks
- **LLMExecutorService**: Add shutdown hook
- **CodeExecutionEngine**: Implement AutoCloseable
- **SimpleEventBus**: Add shutdown() method
- **LLMCache**: Fix infinite loop in eviction

#### 1.2 Fix State Machine Recovery
```java
// In ActionExecutor.java, line 268
} catch (Exception e) {
    MineWrightMod.LOGGER.error("Foreman '{}' failed to get planning result",
        foreman.getSteveName(), e);
    sendToGUI(foreman.getSteveName(), "Oops, something went wrong while planning!");

    // FIX: Reset state machine to IDLE
    if (stateMachine != null) {
        stateMachine.forceTransition(AgentState.IDLE, "planning failed");
    }
}
```

#### 1.3 Fix Blocking Call
```java
// Current (BLOCKING):
ResponseParser.ParsedResponse response = planningFuture.get();

// Fixed (NON-BLOCKING):
planningFuture
    .thenAccept(response -> {
        // Handle success
        handlePlanningResponse(response);
    })
    .exceptionally(error -> {
        // Handle error
        handlePlanningError(error);
        return null;
    });
```

### 9.2 Priority 2: Implement Manual Circuit Breaker

Since Resilience4j has classloading issues:

```java
// Create simple circuit breaker package
// com.minewright.llm.resilience.manual

public class ManualCircuitBreakerIntegration {
    public static AsyncLLMClient wrapWithCircuitBreaker(AsyncLLMClient client) {
        return new CircuitBreakerWrapper(client, new SimpleCircuitBreaker());
    }
}

// Usage in TaskPlanner
AsyncLLMClient openAIClient = ManualCircuitBreakerIntegration.wrapWithCircuitBreaker(
    new AsyncOpenAIClient(apiKey, model, maxTokens, temperature)
);
```

### 9.3 Priority 3: Add Jitter to Retries

```java
// In OpenAIClient.java
private int calculateDelayMs(int attempt) {
    long baseDelay = INITIAL_RETRY_DELAY_MS * (int) Math.pow(2, attempt);
    long jitter = (long) (baseDelay * 0.3 * (Math.random() - 0.5)); // +/- 15%
    return (int) Math.max(0, baseDelay + jitter);
}
```

### 9.4 Priority 4: Implement Result Type

Replace null returns with Result type:

```java
public class Result<T> {
    private final T value;
    private final ErrorType error;
    private final boolean isSuccess;

    private Result(T value, ErrorType error) {
        this.value = value;
        this.error = error;
        this.isSuccess = (error == null);
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    public static <T> Result<T> failure(ErrorType error) {
        return new Result<>(null, error);
    }

    public T getOrElse(T defaultValue) {
        return isSuccess ? value : defaultValue;
    }
}

// Usage in TaskPlanner
public CompletableFuture<Result<ParsedResponse>> planTasksAsync(...) {
    return client.sendAsync(userPrompt, params)
        .thenApply(response -> {
            ParsedResponse parsed = ResponseParser.parseAIResponse(content);
            if (parsed == null) {
                return Result.failure(ErrorType.INVALID_RESPONSE);
            }
            return Result.success(parsed);
        })
        .exceptionally(error -> {
            return Result.failure(classifyError(error));
        });
}
```

### 9.5 Priority 5: Enhanced Action Recovery

```java
// Add retry support to BaseAction
public abstract class RetriableAction extends BaseAction {
    protected int retryCount = 0;
    protected int ticksInRetry = 0;
    protected RetryPolicy retryPolicy;

    @Override
    protected void onTick() {
        if (shouldRetry()) {
            handleRetry();
            return;
        }
        performAction();
    }

    protected boolean shouldRetry() {
        if (retryCount >= retryPolicy.getMaxAttempts()) {
            return false;
        }
        if (lastFailure == null || !retryPolicy.isRetryable(lastFailure)) {
            return false;
        }
        return ticksInRetry < calculateBackoffDelay();
    }
}
```

### 9.6 Priority 6: Multi-Provider Support

```java
// Add provider cascade configuration
[llm.providers]
primary = "openai"
fallback = ["groq", "gemini"]

# Provider-specific settings
[llm.providers.openai]
apiKey = "sk-..."
model = "gpt-4"
priority = 1

[llm.providers.groq]
apiKey = "gsk_..."
model = "llama3-70b-8192"
priority = 2
```

---

## 10. Implementation Roadmap

### Phase 1: Critical Fixes (Week 1)
- [ ] Fix resource leaks (executors, contexts, event bus)
- [ ] Fix state machine not resetting on planning failure
- [ ] Fix blocking `planningFuture.get()` call
- [ ] Add null checks for orchestrator usage
- [ ] Fix CollaborativeBuildManager thread safety

### Phase 2: Resilience Infrastructure (Week 2)
- [ ] Implement manual CircuitBreaker (replace Resilience4j)
- [ ] Add jitter to all retry implementations
- [ ] Implement Result type (replace null returns)
- [ ] Add timeout handling at planning level
- [ ] Implement ProviderHealthMonitor

### Phase 3: Action Recovery (Week 3)
- [ ] Create RetriableAction base class
- [ ] Implement retry policies per action type
- [ ] Add RecoveryAction chain pattern
- [ ] Implement StatefulAction for progress preservation
- [ ] Add failure tracking and metrics

### Phase 4: Enhanced Fallbacks (Week 4)
- [ ] Implement multi-provider cascade
- [ ] Add context-aware fallback handler
- [ ] Implement cached fallback responses
- [ ] Add rule-based action generation
- [ ] Create recovery prompt system

### Phase 5: User Experience (Week 5)
- [ ] Implement notification severity levels
- [ ] Add interactive recovery prompts
- [ ] Implement recovery suggestion system
- [ ] Add failure learning in agent memory
- [ ] Create user-friendly error messages

### Phase 6: Testing & Documentation (Week 6)
- [ ] Unit tests for retry logic
- [ ] Integration tests for error recovery
- [ ] Chaos testing for fault injection
- [ ] Load testing for concurrent failures
- [ ] Update documentation

---

## 11. Key Takeaways

### What MineWright Does Well

1. **Comprehensive Error Classification**: The `LLMException.ErrorType` enum is well-designed
2. **Resilience4j Configuration**: Properly configured with sensible defaults
3. **Pattern-Based Fallbacks**: `LLMFallbackHandler` provides offline capability
4. **Event Listeners**: Good observability for circuit breaker and retry events
5. **Plugin Architecture**: ActionRegistry enables flexible action creation

### What Needs Improvement

1. **Circuit Breaker Unused**: Forge classloading issues prevent Resilience4j usage
2. **Missing Jitter**: Retry delays lack randomness, risking retry storms
3. **Blocking Calls**: `planningFuture.get()` in tick loop is problematic
4. **Null Returns**: TaskPlanner returns null instead of using Result types
5. **State Machine Recovery**: Planning failures leave agent stuck in PLANNING state
6. **Action Recovery**: Actions lack retry policies and recovery chains
7. **Partial Progress**: No state preservation for long-running actions

### Recommended Architecture

```
User Command
    ↓
Cache Check (40% hit rate)
    ↓ (miss)
Circuit Breaker (fast fail if open)
    ↓ (closed)
Primary Provider with Retry+Jitter
    ↓ (failure)
Secondary Provider (Groq/Gemini)
    ↓ (failure)
Pattern-Based Fallback
    ↓ (no match)
Safe Default Action (wait/idle)
    ↓ (exhausted)
User Notification + Escalation
```

---

## 12. Sources and References

### Research Sources

1. [Agent Design Patterns Chapter 12: Exception Handling and Recovery](https://blog.csdn.net/peraglobal/article/details/157220660)
2. [10 Common AI Agent Failure Modes and Fixes](https://m.blog.csdn.net/universsky2015/article/details/156695316)
3. [Google Agent Design Patterns: Exception Handling and Recovery](https://blog.csdn.net/RQfreefly/details/152952462)
4. [Building Reliable LLM Applications: 6 Key Techniques](https://next.hyper.ai/cn/headlines/31e53a64426a56c53e2c810ca54da273)
5. [Multi-Region Active-Active Architecture](https://m.blog.csdn.net/2405_88636357/article/details/145823334)
6. [Circuit Breaker Pattern for AI Applications](https://cloud.tencent.com/developer/article/2581260)
7. [Exponential Backoff with Jitter Implementation](https://blog.csdn.net/2600_94959982/article/details/158277158)
8. [Google Drive API: Truncated Exponential Backoff](https://developers.google.cn/drive/api/guides/limits)

### Internal Documentation

1. `C:\Users\casey\steve\docs\AUDIT_ERROR_HANDLING.md` - Comprehensive error handling audit
2. `C:\Users\casey\steve\docs\ASYNC_ERROR_RECOVERY.md` - Async error recovery patterns
3. `C:\Users\casey\steve\docs\ACTION_RETRY_RECOVERY.md` - Action retry and recovery patterns

### Code References

1. `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\LLMException.java`
2. `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\ResilientLLMClient.java`
3. `C:\Users\casey\steve\src\main\java\com\minewright\llm\resilience\LLMFallbackHandler.java`
4. `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
5. `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java`

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Author:** Research compilation for MineWright project
**Status:** Ready for implementation

---

## Appendix A: Configuration Examples

### config/minewright-common.toml

```toml
[llm]
# Multi-provider configuration
primaryProvider = "openai"
fallbackProviders = ["groq", "gemini"]

# Circuit breaker settings
[llm.circuitBreaker]
enabled = true
failureThreshold = 5
waitDurationSeconds = 30
halfOpenMaxCalls = 3

# Retry settings
[llm.retry]
enabled = true
maxAttempts = 3
initialDelayMs = 1000
backoffMultiplier = 2.0
jitterEnabled = true
jitterPercent = 30

# Action retry settings
[actionRetry]
enabled = true
maxRetries = 3
backoffStrategy = "EXPONENTIAL_JITTER"

# Action-specific overrides
[actionRetry.pathfind]
maxRetries = 5
backoffStrategy = "EXPONENTIAL_JITTER"

[actionRetry.placeBlock]
maxRetries = 5
backoffStrategy = "EXPONENTIAL"

[actionRetry.mine]
maxRetries = 2
backoffStrategy = "IMMEDIATE"

# Recovery settings
[actionRecovery]
enabled = true
promptUser = true
saveProgress = true
progressSaveInterval = 100
```

---

## Appendix B: Testing Checklist

### Unit Tests
- [ ] Circuit breaker state transitions
- [ ] Retry with exponential backoff and jitter
- [ ] Cache hit/miss scenarios
- [ ] Fallback pattern matching
- [ ] Result type success/failure
- [ ] Action recovery chains
- [ ] State snapshot/restore
- [ ] Multi-provider cascade

### Integration Tests
- [ ] Full error recovery flow (cache → provider → fallback)
- [ ] Concurrent circuit breaker behavior
- [ ] Multi-agent coordination failures
- [ ] Partial progress preservation
- [ ] User notification flows

### Chaos Tests
- [ ] Random provider failures
- [ ] Network timeout storms
- [ ] Rate limit exhaustion
- [ ] Concurrent agent conflicts
- [ ] Chunk unload during action

---

**End of Document**
