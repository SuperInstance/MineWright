# Action Execution System - Bug Audit Report

**Date:** 2026-02-27
**Auditor:** Claude Code
**Scope:** ActionExecutor, BaseAction, AgentStateMachine, and related components
**Severity Classification:** Critical, High, Medium, Low

---

## Executive Summary

This audit identified **23 potential bugs and edge cases** across the action execution system. The most critical issues involve:

1. **Memory leaks** from incomplete async operations and event subscriptions
2. **Null pointer exceptions** from missing null checks
3. **Thread safety issues** in collaborative builds and event handling
4. **Resource exhaustion** from unbounded queues and missing cleanup

---

## Table of Contents

1. [Critical Issues](#critical-issues)
2. [High Priority Issues](#high-priority-issues)
3. [Medium Priority Issues](#medium-priority-issues)
4. [Low Priority Issues](#low-priority-issues)
5. [Edge Cases](#edge-cases)
6. [Recommendations](#recommendations)

---

## Critical Issues

### 1. Memory Leak: CompletableFuture Never Cancelled on ActionExecutor Stop

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
**Lines:** 398-412
**Severity:** CRITICAL

**Issue:**
The `stopCurrentAction()` method cancels current actions but does not cancel the pending `planningFuture`. If the user stops the executor while LLM planning is in progress, the CompletableFuture continues running and holds references to the ForemanEntity.

```java
public void stopCurrentAction() {
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
    // BUG: planningFuture is NOT cancelled here!
}
```

**Impact:**
- Memory leak holding ForemanEntity references
- LLM API calls continue after user cancellation
- Wasted API credits and resources

**Fix:**
```java
public void stopCurrentAction() {
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

    // Cancel async planning if in progress
    if (planningFuture != null && !planningFuture.isDone()) {
        planningFuture.cancel(true);
        planningFuture = null;
    }
    isPlanning = false;
    pendingCommand = null;

    // Reset state machine
    stateMachine.reset();
}
```

---

### 2. Memory Leak: EventBus Subscriptions Never Removed

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\execution\EventPublishingInterceptor.java` (referenced)
**Lines:** 73 (ActionExecutor constructor)
**Severity:** CRITICAL

**Issue:**
The `EventPublishingInterceptor` subscribes to the EventBus but the subscription is never cleaned up when the ActionExecutor is destroyed.

```java
interceptorChain.addInterceptor(new EventPublishingInterceptor(eventBus, foreman.getSteveName()));
```

**Impact:**
- Each ForemanEntity adds subscribers to EventBus
- When ForemanEntities are removed, subscribers remain
- Memory leak accumulating dead entity references

**Fix:**
Store the Subscription and clean it up in a disposal method:
```java
private Subscription eventSubscription;

public ActionExecutor(ForemanEntity foreman) {
    // ... existing code ...
    EventPublishingInterceptor eventInterceptor =
        new EventPublishingInterceptor(eventBus, foreman.getSteveName());
    this.eventSubscription = eventInterceptor.getSubscription(); // Requires adding this getter
    interceptorChain.addInterceptor(eventInterceptor);
}

public void dispose() {
    if (eventSubscription != null && eventSubscription.isActive()) {
        eventSubscription.unsubscribe();
    }
    eventBus.clear(); // Or unsubscribe only this foreman's events
    stopCurrentAction();
}
```

---

### 3. Thread Safety: Race Condition in CollaborativeBuildManager

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\CollaborativeBuildManager.java`
**Lines:** 181-210
**Severity:** CRITICAL

**Issue:**
Multiple Foremen can call `getNextBlock()` concurrently, leading to race conditions in section assignment and block retrieval.

```java
public static BlockPlacement getNextBlock(CollaborativeBuild build, String foremanName) {
    if (build.isComplete()) {
        return null;
    }

    build.participatingForemen.add(foremanName);

    // RACE CONDITION: Multiple threads can check and assign simultaneously
    Integer sectionIndex = build.foremanToSectionMap.get(foremanName);
    if (sectionIndex == null) {
        sectionIndex = assignForemanToSection(build, foremanName); // Not atomic!
        if (sectionIndex == null) {
            return null;
        }
    }

    BuildSection section = build.sections.get(sectionIndex);
    BlockPlacement block = section.getNextBlock(); // Atomic, but section selection isn't
    // ...
}
```

**Impact:**
- Multiple Foremen assigned to same section
- Blocks placed multiple times or skipped
- Inconsistent build state

**Fix:**
Use `ConcurrentHashMap.computeIfAbsent()` for atomic section assignment:
```java
public static BlockPlacement getNextBlock(CollaborativeBuild build, String foremanName) {
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

---

### 4. Resource Leak: EventBus Executor Never Shutdown

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\event\SimpleEventBus.java`
**Lines:** 47-54
**Severity:** CRITICAL

**Issue:**
The `SimpleEventBus` creates an ExecutorService but never shuts it down. Each ActionExecutor creates its own EventBus via `new SimpleEventBus()`, creating multiple thread pools that are never terminated.

```java
public SimpleEventBus() {
    this.subscribers = new ConcurrentHashMap<>();
    this.asyncExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "event-bus-async");
        t.setDaemon(true);
        return t;
    });
}
```

**Impact:**
- Thread leak: One thread per ForemanEntity
- Resources never released even after entities removed
- Potential thread pool exhaustion

**Fix:**
Add lifecycle management:
```java
// In ActionExecutor
private final SimpleEventBus eventBus;

public void dispose() {
    if (eventBus != null) {
        eventBus.shutdown();
    }
}

// Or use a shared EventBus instance across all Foremen
private static final EventBus SHARED_EVENT_BUS = new SimpleEventBus();
```

---

## High Priority Issues

### 5. Null Pointer: No Validation in createAction()

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
**Lines:** 350-367
**Severity:** HIGH

**Issue:**
The `createAction()` method returns null for unknown action types, but `executeTask()` assumes non-null.

```java
private void executeTask(Task task) {
    // ...
    currentAction = createAction(task);

    if (currentAction == null) {
        String errorMsg = "Unknown action type: " + task.getAction();
        // Error handling exists HERE
        foreman.sendChatMessage("Error: " + errorMsg);
        return;
    }
    // But tick() doesn't check!
}

public void tick() {
    // ...
    if (currentAction != null) { // Checked here
        if (currentAction.isComplete()) {
            ActionResult result = currentAction.getResult(); // What if result is null?
            // ...
        }
    }
}
```

**Impact:**
- `ActionResult` can be null if `onStart()` sets it to null
- NPE when accessing `result.isSuccess()` or `result.getMessage()`

**Fix:**
Add null checks in `tick()`:
```java
if (currentAction != null) {
    if (currentAction.isComplete()) {
        ActionResult result = currentAction.getResult();
        if (result != null) {
            MineWrightMod.LOGGER.info("Foreman '{}' - Action completed: {} (Success: {})",
                foreman.getSteveName(), result.getMessage(), result.isSuccess());
        }
        // ... rest of code
    }
}
```

---

### 6. Null Pointer: BuildStructureAction.getProgressPercent()

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BuildStructureAction.java`
**Lines:** 516-524
**Severity:** HIGH

**Issue:**
`getProgressPercent()` is called from `ActionExecutor.getCurrentActionProgress()` without checking if `buildPlan` is initialized.

```java
public int getProgressPercent() {
    if (isCollaborative && collaborativeBuild != null) {
        return collaborativeBuild.getProgressPercentage();
    }
    if (buildPlan != null && !buildPlan.isEmpty()) {
        return (currentBlockIndex * 100) / buildPlan.size();
    }
    return 0; // Safe
}

// But in ActionExecutor.getCurrentActionProgress():
if (currentAction instanceof com.minewright.action.actions.BuildStructureAction) {
    com.minewright.action.actions.BuildStructureAction buildAction =
        (com.minewright.action.actions.BuildStructureAction) currentAction;
    return buildAction.getProgressPercent(); // Safe
}
```

**Status:** LOW RISK - Method handles nulls correctly, but issue exists if `buildPlan` is set to null after initialization.

---

### 7. Thread Safety: InterceptorChain Sort During Add

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\execution\InterceptorChain.java`
**Lines:** 67-101
**Severity:** HIGH

**Issue:**
`sortInterceptors()` modifies the `CopyOnWriteArrayList` during `addInterceptor()`, which can cause concurrent modification issues.

```java
public void addInterceptor(ActionInterceptor interceptor) {
    if (interceptor == null) {
        throw new IllegalArgumentException("Interceptor cannot be null");
    }

    interceptors.add(interceptor);
    sortInterceptors(); // Modifies list while other threads might be iterating
}

private void sortInterceptors() {
    List<ActionInterceptor> sorted = new ArrayList<>(interceptors);
    sorted.sort(Comparator.comparingInt(ActionInterceptor::getPriority).reversed());
    interceptors.clear(); // Clears the CopyOnWriteArrayList
    interceptors.addAll(sorted); // Re-adds sorted
}
```

**Impact:**
- Lost updates if interceptor added during iteration
- `executeBeforeAction()` might skip interceptors
- Inconsistent interceptor ordering

**Fix:**
Use a `ConcurrentSkipListSet` for automatic priority-based sorting:
```java
private final ConcurrentSkipListSet<ActionInterceptor> interceptors =
    new ConcurrentSkipListSet<>(Comparator.comparingInt(ActionInterceptor::getPriority).reversed());

public void addInterceptor(ActionInterceptor interceptor) {
    if (interceptor == null) {
        throw new IllegalArgumentException("Interceptor cannot be null");
    }
    interceptors.add(interceptor); // Automatically sorted
}
```

---

### 8. Resource Leak: CollaborativeBuildManager Never Cleans Up

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\CollaborativeBuildManager.java`
**Lines:** 161-284
**Severity:** HIGH

**Issue:**
`activeBuilds` map only removes builds when explicitly completed via `completeBuild()`. If builds are abandoned or errors occur, they accumulate forever.

```java
private static final Map<String, CollaborativeBuild> activeBuilds = new ConcurrentHashMap<>();

public static void completeBuild(String structureId) {
    CollaborativeBuild build = activeBuilds.remove(structureId);
    // Only removes if explicitly called
}
```

**Impact:**
- Memory leak from abandoned builds
- Build IDs using timestamps can collide (unlikely but possible)
- No cleanup mechanism for stale builds

**Fix:**
Add periodic cleanup:
```java
public static void cleanupStaleBuilds() {
    Iterator<Map.Entry<String, CollaborativeBuild>> it = activeBuilds.entrySet().iterator();
    long now = System.currentTimeMillis();
    while (it.hasNext()) {
        Map.Entry<String, CollaborativeBuild> entry = it.next();
        CollaborativeBuild build = entry.getValue();
        // Remove if complete or older than 1 hour
        if (build.isComplete() || (now - extractTimestamp(entry.getKey())) > 3_600_000) {
            it.remove();
            MineWrightMod.LOGGER.info("Cleaned up stale build: {}", entry.getKey());
        }
    }
}
```

---

### 9. Null Pointer: Task Result Not Initialized

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BaseAction.java`
**Lines:** 36-42
**Severity:** HIGH

**Issue:**
`getResult()` can return null if the action completes without setting a result.

```java
public ActionResult getResult() {
    return result; // Can be null!
}

// ActionExecutor.tick() assumes non-null:
ActionResult result = currentAction.getResult();
MineWrightMod.LOGGER.info("Foreman '{}' - Action completed: {} (Success: {})",
    foreman.getSteveName(), result.getMessage(), result.isSuccess());
```

**Impact:**
- NPE when logging completed actions
- Crashes if action doesn't set result before completion

**Fix:**
Initialize result in constructor or provide default:
```java
public ActionResult getResult() {
    if (result == null) {
        return ActionResult.failure("Action completed without result");
    }
    return result;
}
```

---

## Medium Priority Issues

### 10. Race Condition: State Machine Transition

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java`
**Lines:** 178-210
**Severity:** MEDIUM

**Issue:**
State transitions use `compareAndSet()` but don't handle the case where another thread changes the state between validation and CAS.

```java
public boolean transitionTo(AgentState targetState, String reason) {
    // ...
    AgentState fromState = currentState.get();

    if (!canTransitionTo(targetState)) {
        return false;
    }

    // RACE: State might have changed between get() and compareAndSet()
    if (currentState.compareAndSet(fromState, targetState)) {
        // Success
        return true;
    } else {
        LOGGER.warn("[{}] State transition failed: concurrent modification", agentId);
        return false; // Silent failure - caller doesn't retry
    }
}
```

**Impact:**
- Failed transitions are logged but not retried
- ActionExecutor may not realize state change failed
- State machine desynchronization

**Fix:**
Add retry loop:
```java
public boolean transitionTo(AgentState targetState, String reason) {
    if (targetState == null) {
        LOGGER.warn("[{}] Cannot transition to null state", agentId);
        return false;
    }

    int retries = 3;
    while (retries-- > 0) {
        AgentState fromState = currentState.get();

        if (!canTransitionTo(targetState)) {
            LOGGER.warn("[{}] Invalid state transition: {} → {}",
                agentId, fromState, targetState);
            return false;
        }

        if (currentState.compareAndSet(fromState, targetState)) {
            LOGGER.info("[{}] State transition: {} → {}{}",
                agentId, fromState, targetState,
                reason != null ? " (reason: " + reason + ")" : "");

            if (eventBus != null) {
                eventBus.publish(new StateTransitionEvent(agentId, fromState, targetState, reason));
            }
            return true;
        }
    }

    LOGGER.error("[{}] State transition failed after retries: {} → {}",
        agentId, currentState.get(), targetState);
    return false;
}
```

---

### 11. Memory Leak: TaskQueue Never Bounded

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
**Lines:** 38
**Severity:** MEDIUM

**Issue:**
`taskQueue` is an unbounded `LinkedList`. If the LLM generates thousands of tasks, memory grows unbounded.

```java
private final Queue<Task> taskQueue = new LinkedList<>();

// In processNaturalLanguageCommand():
taskQueue.clear();
taskQueue.addAll(response.getTasks()); // Could be thousands!
```

**Impact:**
- Memory exhaustion from large task queues
- No backpressure mechanism for LLM over-generation
- Potential OOM with complex commands

**Fix:**
Use bounded queue with fallback:
```java
private static final int MAX_TASK_QUEUE_SIZE = 1000;
private final Queue<Task> taskQueue = new LinkedList<>();

// In processNaturalLanguageCommand():
taskQueue.clear();
List<Task> tasks = response.getTasks();
if (tasks.size() > MAX_TASK_QUEUE_SIZE) {
    MineWrightMod.LOGGER.warn("Task queue exceeds limit, truncating from {} to {}",
        tasks.size(), MAX_TASK_QUEUE_SIZE);
    tasks = tasks.subList(0, MAX_TASK_QUEUE_SIZE);
}
taskQueue.addAll(tasks);
```

---

### 12. Edge Case: Action Started Multiple Times

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BaseAction.java`
**Lines:** 19-23
**Severity:** MEDIUM

**Issue:**
`start()` can be called multiple times due to the `started` flag check. However, `onStart()` might have side effects that shouldn't repeat.

```java
public void start() {
    if (started) return; // Idempotent
    started = true;
    onStart(); // What if this has side effects?
}
```

**Impact:**
- Actions may initialize resources multiple times
- Teleports, inventory changes, etc. could repeat
- State inconsistency if called multiple times

**Status:** PROTECTED - The `started` flag prevents re-execution, but `onStart()` implementations should also be idempotent.

---

### 13. Resource Leak: ForemanEntity Flying State Not Reset

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BuildStructureAction.java`
**Lines:** 178, 189, 265
**Severity:** MEDIUM

**Issue:**
Multiple code paths set `foreman.setFlying(true)` but error paths might miss setting it back to `false`.

```java
@Override
protected void onTick() {
    ticksRunning++;

    if (ticksRunning > MAX_TICKS) {
        foreman.setFlying(false); // GOOD: Reset on timeout
        result = ActionResult.failure("Building timeout");
        return;
    }
    // ... other paths that don't reset
}

@Override
protected void onCancel() {
    foreman.setFlying(false); // GOOD: Reset on cancel
    foreman.getNavigation().stop();
}
```

**Impact:**
- ForemanEntity stuck in flying mode after errors
- Affects physics and collision
- Gameplay bugs

**Fix:**
Use try-finally pattern:
```java
@Override
protected void onTick() {
    try {
        ticksRunning++;
        // ... logic
    } catch (Exception e) {
        foreman.setFlying(false);
        throw e;
    }
}
```

---

### 14. Null Pointer: MineBlockAction Block Parsing

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\MineBlockAction.java`
**Lines:** 62-75, 362-373
**Severity:** MEDIUM

**Issue:**
`parseBlock()` can return `null` if the block name is invalid, but `onStart()` doesn't fully handle this case.

```java
@Override
protected void onStart() {
    String blockName = task.getStringParameter("block");
    // ...
    targetBlock = parseBlock(blockName);

    if (targetBlock == null || targetBlock == Blocks.AIR) {
        result = ActionResult.failure("Invalid block type: " + blockName);
        return; // GOOD: Early return
    }
    // ...
}

private Block parseBlock(String blockName) {
    // ...
    ResourceLocation resourceLocation = new ResourceLocation(normalizedBlockName);
    return BuiltInRegistries.BLOCK.get(resourceLocation); // Can return null
}
```

**Status:** HANDLED - Code checks for null and returns failure.

---

### 15. Thread Safety: EventBus Publish During Unsubscribe

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\event\SimpleEventBus.java`
**Lines:** 221-229
**Severity:** MEDIUM

**Issue:**
`SubscriptionImpl.unsubscribe()` sets `active` to false and removes from list, but `publish()` iterates without synchronization.

```java
// In unsubscribe():
public void unsubscribe() {
    entry.active.set(false);
    CopyOnWriteArrayList<SubscriberEntry<?>> list = subscribers.get(eventType);
    if (list != null) {
        list.remove(entry); // Removes while publish() might be iterating
    }
}

// In publish():
for (SubscriberEntry<?> entry : subs) {
    if (!entry.isActive()) continue; // Checks active flag
    try {
        ((Consumer<T>) entry.subscriber).accept(event);
    } catch (Exception e) {
        // ...
    }
}
```

**Impact:**
- Events delivered to unsubscribed handlers (if not removed before iteration)
- `ConcurrentModificationException` unlikely due to `CopyOnWriteArrayList`

**Status:** SAFE - `CopyOnWriteArrayList` handles concurrent modification, and `isActive()` check prevents delivery to unsubscribed handlers.

---

## Low Priority Issues

### 16. Edge Case: TicksSinceLastAction Overflow

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
**Lines:** 42, 226
**Severity:** LOW

**Issue:**
`ticksSinceLastAction` is an `int` that increments every tick. After ~68 days at 20 TPS, it would overflow.

```java
private int ticksSinceLastAction;

public void tick() {
    ticksSinceLastAction++; // Overflows after ~2.1 billion ticks
    // ...
}
```

**Impact:**
- Negative values after overflow
- Action delay logic breaks (comparison with `ACTION_TICK_DELAY`)
- Extremely rare in practice

**Fix:**
Use modulo or reset:
```java
public void tick() {
    ticksSinceLastAction++;
    if (ticksSinceLastAction < 0) { // Overflow detected
        ticksSinceLastAction = 0;
    }
    // ...
}
```

---

### 17. Edge Case: ActionResult Message Not Checked for Null

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionResult.java`
**Lines:** 8-16
**Severity:** LOW

**Issue:**
`ActionResult` doesn't validate that `message` is non-null.

```java
public ActionResult(boolean success, String message, boolean requiresReplanning) {
    this.success = success;
    this.message = message; // Can be null!
    this.requiresReplanning = requiresReplanning;
}

public String getMessage() {
    return message; // Returns null
}
```

**Impact:**
- NPE when logging `result.getMessage()`
- Null messages displayed to user

**Fix:**
Add validation in constructor:
```java
public ActionResult(boolean success, String message, boolean requiresReplanning) {
    this.success = success;
    this.message = message != null ? message : (success ? "Success" : "Failed");
    this.requiresReplanning = requiresReplanning;
}
```

---

### 18. Edge Case: AgentStateMachine Name Not Validated

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java`
**Lines:** 128-133
**Severity:** LOW

**Issue:**
`agentId` is not validated for null or empty strings.

```java
public AgentStateMachine(EventBus eventBus, String agentId) {
    this.currentState = new AtomicReference<>(AgentState.IDLE);
    this.eventBus = eventBus;
    this.agentId = agentId; // Could be null!
    LOGGER.debug("[{}] State machine initialized in IDLE state", agentId);
}
```

**Impact:**
- Null strings in logs
- Confusing log output

**Fix:**
```java
public AgentStateMachine(EventBus eventBus, String agentId) {
    this.currentState = new AtomicReference<>(AgentState.IDLE);
    this.eventBus = eventBus;
    this.agentId = agentId != null && !agentId.isEmpty() ? agentId : "unknown";
    LOGGER.debug("[{}] State machine initialized in IDLE state", this.agentId);
}
```

---

### 19. Edge Case: ActionExecutor.getCurrentActionProgress()

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
**Lines:** 490-504
**Severity:** LOW

**Issue:**
Only `BuildStructureAction` progress is tracked. Other actions return default values.

```java
public int getCurrentActionProgress() {
    if (currentAction == null) {
        return taskQueue.isEmpty() ? 100 : 0;
    }

    // Only BuildStructureAction has custom progress
    if (currentAction instanceof com.minewright.action.actions.BuildStructureAction) {
        // ...
        return buildAction.getProgressPercent();
    }

    // Default to 50% if action is in progress
    return currentAction.isComplete() ? 100 : 50;
}
```

**Impact:**
- Inaccurate progress reporting for non-build actions
- User confusion about completion status

**Status:** ACCEPTABLE - Default values are reasonable for UI purposes.

---

### 20. Edge Case: IdleFollowAction Memory Leak

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
**Lines:** 304-319
**Severity:** LOW

**Issue:**
`IdleFollowAction` is recreated repeatedly but the old instances may not be fully cleaned up.

```java
if (taskQueue.isEmpty() && currentAction == null && currentGoal == null) {
    if (idleFollowAction == null) {
        idleFollowAction = new IdleFollowAction(foreman);
        idleFollowAction.start();
    } else if (idleFollowAction.isComplete()) {
        // Old action is complete, creating new one
        idleFollowAction = new IdleFollowAction(foreman);
        idleFollowAction.start();
    } else {
        idleFollowAction.tick();
    }
}
```

**Impact:**
- Old `IdleFollowAction` instances remain in memory
- Navigation paths not cleared

**Fix:**
Explicitly cancel before reassigning:
```java
} else if (idleFollowAction.isComplete()) {
    idleFollowAction.cancel(); // Clean up old instance
    idleFollowAction = new IdleFollowAction(foreman);
    idleFollowAction.start();
}
```

---

## Edge Cases

### 21. Concurrent Command Processing

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
**Lines:** 117-162
**Severity:** MEDIUM

**Scenario:**
User presses command key twice rapidly before first command completes planning.

**Current Behavior:**
```java
if (isPlanning) {
    MineWrightMod.LOGGER.warn("Foreman '{}' is already planning, ignoring command: {}", ...);
    sendToGUI(foreman.getSteveName(), "Hold on, I'm still thinking about the previous command...");
    return;
}
```

**Issue:**
- Second command is silently dropped
- User doesn't know if command was queued or ignored
- No command queue for pending commands

**Recommendation:**
Add a command queue or replace pending command:
```java
if (isPlanning) {
    // Option 1: Queue the command
    pendingCommands.offer(command);

    // Option 2: Replace the pending command
    MineWrightMod.LOGGER.warn("Foreman '{}' replacing pending command", foreman.getSteveName());
    if (planningFuture != null && !planningFuture.isDone()) {
        planningFuture.cancel(true);
    }
    // Continue with new command
}
```

---

### 22. Network Partition During Async Planning

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\TaskPlanner.java`
**Lines:** 163-210
**Severity:** MEDIUM

**Scenario:**
Network connection drops during LLM API call.

**Current Behavior:**
```java
.exceptionally(throwable -> {
    MineWrightMod.LOGGER.error("[Async] Error planning tasks: {}", throwable.getMessage());
    return null; // Returns null on any error
});
```

**Issue:**
- `null` returned to ActionExecutor
- User sees generic "couldn't understand" message
- No distinction between network error and parse error

**Recommendation:**
Return error result instead of null:
```java
.exceptionally(throwable -> {
    MineWrightMod.LOGGER.error("[Async] Error planning tasks: {}", throwable.getMessage());
    // Return a failure result instead of null
    ResponseParser.ParsedResponse errorResponse = new ResponseParser.ParsedResponse();
    errorResponse.plan = "Error: " + throwable.getMessage();
    errorResponse.tasks = Collections.emptyList();
    return errorResponse;
});
```

---

### 23. Entity Deserialization After Save/Load

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`
**Severity:** HIGH

**Scenario:**
Game saves while actions are executing, then loads later.

**Issues:**
- `CompletableFuture` is not serializable
- `planningFuture` would be null after load
- `currentAction` may be in inconsistent state
- `isPlanning` flag may be true but no future exists

**Recommendation:**
Add reset on entity load:
```java
@EventListener
public void onEntityLoad(EntityEvent.EntityLoadEvent event) {
    if (event.getEntity() instanceof ForemanEntity) {
        ForemanEntity foreman = (ForemanEntity) event.getEntity();
        ActionExecutor executor = foreman.getActionExecutor();
        if (executor != null) {
            executor.resetAfterLoad();
        }
    }
}

public void resetAfterLoad() {
    planningFuture = null;
    isPlanning = false;
    pendingCommand = null;
    if (currentAction != null) {
        currentAction.cancel();
        currentAction = null;
    }
    stateMachine.reset();
}
```

---

## Recommendations

### Immediate Actions (Critical)

1. **Fix CompletableFuture cleanup** in `stopCurrentAction()`
2. **Add EventBus shutdown** mechanism or use shared instance
3. **Fix CollaborativeBuildManager race condition** with atomic section assignment
4. **Add lifecycle management** for ActionExecutor (dispose/cleanup)

### Short Term (High Priority)

5. Add null validation in `ActionResult` constructor
6. Implement bounded task queue
7. Add retry logic to state machine transitions
8. Fix InterceptorChain concurrent modification

### Medium Term

9. Add command queue for pending user commands
10. Implement better error reporting for async failures
11. Add entity load/reset handlers for save/load
12. Implement periodic cleanup of stale builds

### Code Quality Improvements

13. Add unit tests for concurrent scenarios
14. Add integration tests for action lifecycle
15. Document thread safety guarantees
16. Add monitoring for memory leaks

---

## Testing Recommendations

### Unit Tests Needed

```java
// Test concurrent planning cancellation
@Test
public void testStopDuringPlanning() {
    ActionExecutor executor = new ActionExecutor(foreman);
    executor.processNaturalLanguageCommand("mine diamond");
    executor.stopCurrentAction();
    assertNull(executor.planningFuture);
    assertFalse(executor.isPlanning);
}

// Test race condition in collaborative builds
@Test
public void testConcurrentBlockAssignment() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(2);
    // Spawn two threads getting blocks simultaneously
    // Verify they get different blocks
}

// Test null ActionResult handling
@Test
public void testNullActionResult() {
    BaseAction action = new TestAction(foreman, task);
    action.start();
    action.tick(); // Force completion
    // Verify no NPE when getResult() returns null
}
```

### Integration Tests Needed

```java
// Test entity save/load during action execution
@Test
public void testSaveLoadDuringAction() {
    ForemanEntity foreman = spawnForeman();
    foreman.getActionExecutor().processNaturalLanguageCommand("build house");
    waitForPlanningComplete();

    // Save and reload
    MinecraftServer server = getServer();
    server.saveAllChunks(true, true);

    // Reload entity
    // Verify executor state is reset
}

// Test memory leak detection
@Test
public void testNoMemoryLeakOnEntityRemove() {
    List<ForemanEntity> foremen = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        foremen.add(spawnForeman());
    }

    long before = getMemoryUsage();
    foremen.forEach(f -> f.remove());
    System.gc();
    long after = getMemoryUsage();

    assertTrue(after < before * 1.1); // Less than 10% increase
}
```

---

## Conclusion

The action execution system is well-designed but has several critical issues related to resource cleanup and thread safety. The most urgent fixes needed are:

1. **Memory leaks** from uncanceled futures and event subscriptions
2. **Thread safety** in collaborative builds and state transitions
3. **Resource leaks** from unbounded queues and unreleased executors

Implementing the recommended fixes will significantly improve system stability and prevent production issues.

---

**Audit Completed:** 2026-02-27
**Next Review:** After critical fixes are implemented
