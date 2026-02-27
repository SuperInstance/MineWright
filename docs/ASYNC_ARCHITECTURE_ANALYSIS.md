# Async Architecture Analysis - Visual Overview

**Companion to:** REFINE_ASYNC_PATTERNS.md

---

## Current Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Game Thread (Main)                       │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    ActionExecutor                          │ │
│  │                                                            │ │
│  │  processNaturalLanguageCommand()                          │ │
│  │         │                                                  │ │
│  │         ▼                                                  │ │
│  │  planningFuture = taskPlanner.planTasksAsync()            │ │
│  │         │                                                  │ │
│  │         └────────────────── returns immediately ──────────┘ │
│  │                                                            │ │
│  │  tick()                                                    │ │
│  │         │                                                  │ │
│  │         ▼                                                  │ │
│  │  if (planningFuture.isDone())                              │ │
│  │      response = planningFuture.get() ❌ BLOCKS HERE        │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ CompletableFuture
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Thread Pools (LLMExecutorService)            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐    │
│  │ openai-exec │  │ groq-exec   │  │ gemini-exec         │    │
│  │ (5 threads) │  │ (5 threads) │  │ (5 threads)         │    │
│  └─────────────┘  └─────────────┘  └─────────────────────┘    │
│         │                 │                     │                │
│         └─────────────────┴─────────────────────┘                │
│                           │                                      │
│                           ▼                                      │
│  ⚠️ ISSUE: Never shut down - threads leak on server restart    │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ HTTP Request
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Async OpenAI Client                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  sendAsync()                                             │  │
│  │         │                                                │  │
│  │         ▼                                                │  │
│  │  httpClient.sendAsync()                                  │  │
│  │         │                                                │  │
│  │         ▼                                                │  │
│  │  sendWithRetry()                                         │  │
│  │         │                                                │  │
│  │         ▼                                                │  │
│  │  ⚠️ ISSUE: Static RETRY_SCHEDULER never shut down       │  │
│  │  (newSingleThreadScheduledExecutor)                      │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Thread Lifecycle Issues

```
Normal Lifecycle (What should happen):
┌──────────┐     ┌────────────┐     ┌───────────┐     ┌──────────┐
│  Create  │ ──► │  Execute   │ ──► │ Shutdown  │ ──► │ Cleanup  │
└──────────┘     └────────────┘     └───────────┘     └──────────┘

Current Lifecycle (What's happening):
┌──────────┐     ┌────────────┐     ┌─────────────────────────────┐
│  Create  │ ──► │  Execute   │ ──► │ Orphaned on server restart  │
└──────────┘     └────────────┘     └─────────────────────────────┘
                                               │
                                               ▼
                                         Thread Leak!
                                         (threads continue running)
```

---

## Cancellation Flow Issues

```
Current Behavior:
┌────────────────┐     processNaturalLanguageCommand()
│ User sends     │ ──────────────────────────────────────┐
│ command A      │                                       │
└────────────────┘                                       │
                                                         │
                     planningFuture = planTasksAsync()    │
                                │                        │
                                ▼                        │
                     ┌────────────────────┐             │
                     │ LLM processing     │ ◄── takes 30-60s
                     │ (in background)    │             │
                     └────────────────────┘             │
                                │                        │
┌────────────────┐               │                        │
│ User sends     │ ──────────────┘                        │
│ command B      │                                       │
└────────────────┘                                       │
        │                                                 │
        ▼                                                 │
 ❌ "Already planning, ignoring..."                       │
                                                         │
User must wait 30-60s before sending another command   ──┘

Improved Behavior:
┌────────────────┐     processNaturalLanguageCommand()
│ User sends     │ ──────────────────────────────────────┐
│ command A      │                                       │
└────────────────┘                                       │
                                                         │
                     planningFuture = planTasksAsync()    │
                                │                        │
                                ▼                        │
                     ┌────────────────────┐             │
                     │ LLM processing     │             │
                     │ (in background)    │             │
                     └────────────────────┘             │
                                │                        │
┌────────────────┐               │                        │
│ User sends     │ ──────────────┘                        │
│ command B      │                                       │
└────────────────┘                                       │
        │                                                 │
        ▼                                                 │
 ✓ planningFuture.cancel(true) ────────────────────────┤
        │                                                 │
        ▼                                                 │
 "Previous command cancelled, starting new one..."       │
                                                         │
New planning starts immediately ─────────────────────────┘
```

---

## Resource Leak Analysis

### Memory Leaks

| Component | Leak Source | Impact | Fix |
|-----------|-------------|--------|-----|
| LLMExecutorService | Threads never shutdown | 15 threads (3 pools × 5) | Add shutdown hook |
| AsyncOpenAIClient | Static RETRY_SCHEDULER | 1 thread per class load | Make instance-based |
| HeartbeatScheduler | CachedThreadPool | Unbounded threads | Use bounded pool |
| BatchingLLMClient | Untracked futures | Orphaned requests | Track and cancel |
| TaskPlanner | BatchingClient not stopped | Thread + queue | Call shutdown() |
| LLMCache | No size limit monitoring | Potential OOM | Already has limit |

### Thread Leak Timeline

```
Server Start:
  └─> LLMExecutorService: 3 thread pools (15 threads)
  └─> HeartbeatScheduler: 1 thread
  └─> AsyncOpenAIClient: 1 retry scheduler thread
  └─> BatchingLLMClient: 2 schedulers
  └─> Total: ~20 threads

After 10 Mod Reloads (without fixes):
  └─> 20 × 10 = 200 orphaned threads
  └─> Memory usage: ~50-100MB just for thread stacks
  └─→ Server performance degrades
```

---

## Timeout Hierarchy

```
Application Level (not currently enforced):
┌─────────────────────────────────────────┐
│  ABSOLUTE_TIMEOUT: 90 seconds           │
│  ┌───────────────────────────────────┐  │
│  │  HTTP_TIMEOUT: 30s (OpenAI/Groq) │  │
│  │  HTTP_TIMEOUT: 60s (Gemini)      │  │
│  │  ┌───────────────────────────┐   │  │
│  │  │  RETRY: 1s + 2s + 4s      │   │  │
│  │  │  Total: ~7s               │   │  │
│  │  └───────────────────────────┘   │  │
│  │                                   │  │
│  │  Max per request: 37s (OpenAI)    │  │
│  │  Max per request: 67s (Gemini)    │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘

Issue: No application-level timeout means:
  - Infinite waits possible if future never completes
  - ActionExecutor.tick() can block game thread
  - No recovery from hung LLM calls
```

---

## CompletableFuture Anti-Patterns Found

### Pattern 1: Blocking in Game Thread (❌ BAD)
```java
// In ActionExecutor.tick()
if (planningFuture.isDone()) {
    response = planningFuture.get();  // ❌ Can block indefinitely!
}
```

**Fix:**
```java
response = planningFuture.get(100, TimeUnit.MILLISECONDS);  // ✓ Timeout
```

### Pattern 2: Ignoring Future (❌ BAD)
```java
// In BatchingLLMClient.sendBatch()
underlyingClient.sendAsync(batch.userPrompt, params)
    .thenAccept(response -> handleSuccess(batch, response))
    .exceptionally(error -> handleError(batch, error));
// ❌ Future not stored - can't cancel!
```

**Fix:**
```java
CompletableFuture<LLMResponse> future = underlyingClient.sendAsync(...)
pendingBatches.put(batch.id, future);  // ✓ Track for cancellation
```

### Pattern 3: Static Executor (❌ BAD)
```java
// In AsyncOpenAIClient
private static final ScheduledExecutorService RETRY_SCHEDULER =
    Executors.newSingleThreadScheduledExecutor(...);
// ❌ Never shut down!
```

**Fix:**
```java
private final ScheduledExecutorService retryScheduler;  // ✓ Instance field
// Implement AutoCloseable.close() to shutdown
```

### Pattern 4: Swallowing Exceptions (❌ BAD)
```java
.catch(Exception e) {
    // ❌ Loses specific error type
    MineWrightMod.LOGGER.error("Error", e);
}
```

**Fix:**
```java
.catch(ExecutionException e) {
    Throwable cause = e.getCause();  // ✓ Unwrap for actual cause
    if (cause instanceof LLMException) {
        // Handle LLM-specific errors
    }
}
```

---

## Priority Matrix

| Issue | Severity | Effort | Impact | Priority |
|-------|----------|--------|--------|----------|
| Blocking get() in tick() | High | Low | High | **P0** |
| Thread pool leaks | High | Low | High | **P0** |
| No cancellation support | Medium | Medium | High | **P1** |
| Missing timeouts | Medium | Medium | Medium | **P1** |
| No retry logic (Groq/Gemini) | Medium | High | Medium | **P2** |
| Missing metrics | Low | Low | Low | **P3** |
| Request tracing | Low | Medium | Low | **P3** |

---

## Quick Reference: Key Files to Modify

```
Must Fix (P0):
├── src/main/java/com/minewright/llm/async/LLMExecutorService.java
│   └── Add shutdown hook + improve shutdown()
├── src/main/java/com/minewright/action/ActionExecutor.java
│   └── Fix blocking get() → get(timeout)
└── src/main/java/com/minewright/llm/TaskPlanner.java
    └── Call shutdown() from entity cleanup

Should Fix (P1):
├── src/main/java/com/minewright/llm/async/AsyncOpenAIClient.java
│   └── Fix retry scheduler (instance-based)
├── src/main/java/com/minewright/llm/batch/BatchingLLMClient.java
│   └── Track pending futures
├── src/main/java/com/minewright/llm/batch/HeartbeatScheduler.java
│   └── Use bounded thread pool
└── src/main/java/com/minewright/llm/async/TimeoutConfig.java
    └── NEW: Centralized timeout config

Nice to Have (P2/P3):
├── src/main/java/com/minewright/llm/async/AsyncGroqClient.java
│   └── Add retry logic
└── src/main/java/com/minewright/llm/async/AsyncGeminiClient.java
    └── Add retry logic
```
