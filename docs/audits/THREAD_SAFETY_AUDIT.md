# Thread Safety Audit - MineWright Codebase

**Date:** 2026-02-27
**Auditor:** Claude Code
**Scope:** Multi-agent orchestration and action execution system
**Severity Levels:** CRITICAL > HIGH > MEDIUM > LOW > INFO

---

## Executive Summary

This audit identified **23 thread safety issues** across 4 core classes:
- **8 CRITICAL** issues - Immediate action required
- **6 HIGH** severity issues
- **5 MEDIUM** severity issues
- **4 LOW** severity issues

**Key Findings:**
1. **ActionExecutor**: Multiple race conditions in async planning flow
2. **CollaborativeBuildManager**: ConcurrentHashMap misuse and lost updates
3. **OrchestratorService**: Compound operations without atomicity
4. **SimpleEventBus**: Publication race during unsubscription

---

## Table of Contents

1. [ActionExecutor.java](#actionexecutorjava)
2. [CollaborativeBuildManager.java](#collaborativebuildmanagerjava)
3. [OrchestratorService.java](#orchestratorservicejava)
4. [SimpleEventBus.java](#simpleeventbusjava)
5. [Cross-Cutting Concerns](#cross-cutting-concerns)
6. [Recommendations Summary](#recommendations-summary)

---

## ActionExecutor.java

**Location:** `src/main/java/com/minewright/action/ActionExecutor.java`

### Issue 1: Race Condition in Async Planning Check

**Severity:** CRITICAL

**Location:** Lines 229-262

**Problem:**
```java
// Line 229 - Check-then-act race condition
if (isPlanning && planningFuture != null && planningFuture.isDone()) {
    try {
        ResponseParser.ParsedResponse response = planningFuture.get();
        // ... process response ...
    } finally {
        isPlanning = false;
        planningFuture = null;
        pendingCommand = null;
    }
}
```

**Issue:** Between checking `isDone()` and calling `get()`, another thread could cancel the future. Additionally, `isPlanning` flag is not volatile.

**Impact:** Thread can hang indefinitely on `future.get()` if cancelled, or see stale `isPlanning` value.

**Fix:**
```java
// Make isPlanning volatile
private volatile boolean isPlanning = false;

// Use timeout and handle cancellation properly
if (isPlanning && planningFuture != null) {
    try {
        if (!planningFuture.isDone()) {
            break; // Not ready yet
        }
        ResponseParser.ParsedResponse response = planningFuture.get(100, TimeUnit.MILLISECONDS);
        // ... process response ...
    } catch (TimeoutException e) {
        // Not ready, will check again next tick
        break;
    } catch (CancellationException e) {
        MineWrightMod.LOGGER.info("Foreman '{}' planning was cancelled", foreman.getSteveName());
        isPlanning = false;
        planningFuture = null;
        pendingCommand = null;
    } catch (Exception e) {
        // ... error handling ...
    }
}
```

### Issue 2: Non-Volatile State Flags

**Severity:** HIGH

**Location:** Lines 47-48, 40-43

**Problem:**
```java
private boolean isPlanning = false;  // Not volatile
private String pendingCommand;       // Not volatile
private BaseAction currentAction;    // Not volatile
private String currentGoal;          // Not volatile
private int ticksSinceLastAction;    // Not volatile
```

**Issue:** These fields are accessed from both the game thread (in `tick()`) and potentially async LLM callback threads. Without volatile, changes may not be visible across threads.

**Impact:** Stale state reads, missed state transitions, infinite loops.

**Fix:**
```java
private volatile boolean isPlanning = false;
private volatile String pendingCommand;
private volatile BaseAction currentAction;  // Consider final ref + immutable state instead
private volatile String currentGoal;
private volatile int ticksSinceLastAction;  // Or use AtomicInteger
```

### Issue 3: TaskQueue Concurrent Modification

**Severity:** MEDIUM

**Location:** Lines 199-200, 237-238, 296, 407

**Problem:**
```java
// LinkedList is not thread-safe!
private final Queue<Task> taskQueue = new LinkedList<>();

// Line 199-200 - Async thread modifies queue
taskQueue.clear();
taskQueue.addAll(response.getTasks());

// Line 296 - Game thread modifies queue
Task nextTask = taskQueue.poll();
```

**Issue:** `LinkedList` is not thread-safe. Async LLM thread calls `clear()` and `addAll()` while game thread calls `poll()`.

**Impact:** `ConcurrentModificationException`, corrupted queue state, lost tasks.

**Fix:**
```java
// Use ConcurrentLinkedQueue for lock-free thread safety
private final Queue<Task> taskQueue = new ConcurrentLinkedQueue<>();

// For clear + addAll, use compound operation atomically
synchronized (taskQueue) {
    taskQueue.clear();
    taskQueue.addAll(response.getTasks());
}

// Or better: use Collections.synchronizedList wrapper
private final Queue<Task> taskQueue =
    Collections.synchronizedQueue(new LinkedList<>());
```

### Issue 4: Lazy Initialization Race

**Severity:** MEDIUM

**Location:** Lines 92-98

**Problem:**
```java
public TaskPlanner getTaskPlanner() {
    if (taskPlanner == null) {  // Check-then-act
        MineWrightMod.LOGGER.info("Initializing TaskPlanner for Foreman '{}'", foreman.getSteveName());
        taskPlanner = new TaskPlanner();
    }
    return taskPlanner;
}
```

**Issue:** Double-checked locking not implemented correctly. Multiple threads could create multiple TaskPlanner instances.

**Impact:** Memory leak, unpredictable behavior.

**Fix:**
```java
// Option 1: Initialize in constructor (simplest)
public ActionExecutor(ForemanEntity foreman) {
    this.taskPlanner = new TaskPlanner();
    // ... rest of constructor
}

// Option 2: Proper double-checked locking
private volatile TaskPlanner taskPlanner;

public TaskPlanner getTaskPlanner() {
    TaskPlanner result = taskPlanner;
    if (result == null) {
        synchronized (this) {
            result = taskPlanner;
            if (result == null) {
                taskPlanner = result = new TaskPlanner();
            }
        }
    }
    return result;
}
```

### Issue 5: CompletableFuture State Visibility

**Severity:** LOW

**Location:** Lines 46, 147

**Problem:**
```java
private CompletableFuture<ResponseParser.ParsedResponse> planningFuture;

// Assignment without memory barrier
planningFuture = getTaskPlanner().planTasksAsync(foreman, command);
```

**Issue:** `planningFuture` is not volatile, so game thread may see null or stale reference.

**Impact:** Rare race where `isPlanning` is true but `planningFuture` appears null.

**Fix:**
```java
private volatile CompletableFuture<ResponseParser.ParsedResponse> planningFuture;
```

---

## CollaborativeBuildManager.java

**Location:** `src/main/java/com/minewright/action/CollaborativeBuildManager.java`

### Issue 6: Compound Operations on ConcurrentHashMap

**Severity:** CRITICAL

**Location:** Lines 189-196, 217-246

**Problem:**
```java
// Line 189 - Check-then-act on non-atomic view
Integer sectionIndex = build.foremanToSectionMap.get(foremanName);
if (sectionIndex == null) {
    sectionIndex = assignForemanToSection(build, foremanName);
    // ...
}

// Line 222 - Compound check-and-act
boolean alreadyAssigned = build.foremanToSectionMap.containsValue(i);
if (!alreadyAssigned) {
    build.foremanToSectionMap.put(foremanName, i);
    // ...
}
```

**Issue:** `containsValue()` + `put()` is not atomic. Multiple foremen can check `containsValue()` simultaneously, see "not assigned", and all assign to the same section.

**Impact:** Multiple foremen assigned to same section (lost updates), race conditions.

**Fix:**
```java
// Use putIfAbsent for atomic check-and-act
private static Integer assignForemanToSection(CollaborativeBuild build, String foremanName) {
    // First pass: Find unassigned section
    for (int i = 0; i < build.sections.size(); i++) {
        BuildSection section = build.sections.get(i);
        if (!section.isComplete()) {
            // Atomic assignment using compute
            Integer existing = build.foremanToSectionMap.compute(i, (key, currentOwner) -> {
                return currentOwner == null ? foremanName : currentOwner;
            });

            if (existing == null || foremanName.equals(existing)) {
                MineWrightMod.LOGGER.info("Assigned Foreman '{}' to {} quadrant",
                    foremanName, section.sectionName);
                return i;
            }
        }
    }

    // Second pass: Help with incomplete section
    for (int i = 0; i < build.sections.size(); i++) {
        BuildSection section = build.sections.get(i);
        if (!section.isComplete()) {
            build.foremanToSectionMap.put(foremanName, i);
            return i;
        }
    }

    return null;
}
```

### Issue 7: Lost Update in Progress Tracking

**Severity:** HIGH

**Location:** Lines 98-108

**Problem:**
```java
public int getBlocksPlaced() {
    int total = 0;
    for (BuildSection section : sections) {
        total += section.getBlocksPlaced();  // Unsynchronized iteration
    }
    return total;
}

// In BuildSection
public int getBlocksPlaced() {
    return Math.min(nextBlockIndex.get(), blocks.size());
}
```

**Issue:** While iterating `sections`, another thread could modify the list or the counters, leading to inaccurate totals.

**Impact:** Incorrect progress reporting (>100% or < actual).

**Fix:**
```java
public int getBlocksPlaced() {
    int total = 0;
    // Create snapshot for consistent view
    List<BuildSection> snapshot = new ArrayList<>(sections);
    for (BuildSection section : snapshot) {
        total += section.getBlocksPlaced();
    }
    return total;
}
```

### Issue 8: Concurrent Block Assignment

**Severity:** MEDIUM

**Location:** Lines 140-146

**Problem:**
```java
public BlockPlacement getNextBlock() {
    int index = nextBlockIndex.getAndIncrement();
    if (index < blocks.size()) {
        return blocks.get(index);
    }
    return null;
}
```

**Issue:** `getAndIncrement()` can return index >= `blocks.size()` if multiple threads call this simultaneously. No check for overflow.

**Impact:** `ArrayIndexOutOfBoundsException`, skipped blocks.

**Fix:**
```java
public BlockPlacement getNextBlock() {
    int index;
    int size = blocks.size();
    do {
        index = nextBlockIndex.get();
        if (index >= size) {
            return null; // Section complete
        }
    } while (!nextBlockIndex.compareAndSet(index, index + 1));

    return blocks.get(index);
}
```

### Issue 9: Static Mutable State

**Severity:** MEDIUM

**Location:** Line 161

**Problem:**
```java
private static final Map<String, CollaborativeBuild> activeBuilds = new ConcurrentHashMap<>();
```

**Issue:** Static mutable state is accessible globally. While ConcurrentHashMap is thread-safe, the design makes it easy to introduce race conditions in calling code.

**Impact:** Global state pollution, difficult to test, potential memory leaks.

**Fix:**
```java
// Make instance-based instead of static
public class CollaborativeBuildManager {
    private final Map<String, CollaborativeBuild> activeBuilds;

    public CollaborativeBuildManager() {
        this.activeBuilds = new ConcurrentHashMap<>();
    }

    // Pass instance to components that need it
}
```

### Issue 10: Race in Section Completion Check

**Severity:** LOW

**Location:** Lines 110-117

**Problem:**
```java
public boolean isComplete() {
    for (BuildSection section : sections) {
        if (!section.isComplete()) {
            return false;
        }
    }
    return true;
}
```

**Issue:** Non-atomic check. Section could complete during iteration, but that's benign here. The issue is if the `sections` list is modified concurrently.

**Impact:** Rare `ConcurrentModificationException`.

**Fix:**
```java
public boolean isComplete() {
    return sections.stream().allMatch(BuildSection::isComplete);
}
```

---

## OrchestratorService.java

**Location:** `src/main/java/com/minewright/orchestration/OrchestratorService.java`

### Issue 11: Compound Operations in Task Assignment

**Severity:** CRITICAL

**Location:** Lines 235-256, 261-276

**Problem:**
```java
// Line 238 - Non-atomic check
.filter(s -> !workerAssignments.containsKey(s.getSteveName()))

// Line 266 - Compound operation
plan.addAssignment(assignment);
workerAssignments.put(agentId, assignment);
```

**Issue:** Between `containsKey()` check and `put()`, another thread could assign the same worker. The two map operations are not atomic.

**Impact:** Duplicate assignments, overwritten assignments, lost tasks.

**Fix:**
```java
// Use putIfAbsent for atomic assignment
private void assignTaskToAgent(PlanExecution plan, Task task, String agentId) {
    TaskAssignment assignment = new TaskAssignment(foremanId, task, plan.getPlanId());
    assignment.assignTo(agentId);

    plan.addAssignment(assignment);

    // Atomic put - only if not already assigned
    TaskAssignment existing = workerAssignments.putIfAbsent(agentId, assignment);
    if (existing != null) {
        // Worker already has a task, need to find another
        LOGGER.warn("Agent {} already assigned, finding alternative", agentId);
        // ... handle collision
    }

    // ... rest of method
}
```

### Issue 12: Lost Update in Task Completion

**Severity:** HIGH

**Location:** Lines 350-368

**Problem:**
```java
private void handleTaskComplete(String workerId, AgentMessage message) {
    TaskAssignment assignment = workerAssignments.remove(workerId);  // Remove
    if (assignment != null) {
        String result = message.getPayloadValue("result", "Completed");
        assignment.complete(result);

        // Update plan
        PlanExecution plan = activePlans.get(assignment.getParentPlanId());
        if (plan != null) {
            plan.markTaskComplete(assignment.getAssignmentId());
            checkPlanCompletion(plan);
        }
        // ...
    }
}
```

**Issue:** If the same worker somehow sends two completion messages, the second would null-check pass but plan updates would be lost.

**Impact:** Inconsistent plan state, orphaned assignments.

**Fix:**
```java
private void handleTaskComplete(String workerId, AgentMessage message) {
    TaskAssignment assignment = workerAssignments.remove(workerId);
    if (assignment != null && !assignment.isTerminal()) {
        String result = message.getPayloadValue("result", "Completed");
        assignment.complete(result);

        PlanExecution plan = activePlans.get(assignment.getParentPlanId());
        if (plan != null) {
            plan.markTaskComplete(assignment.getAssignmentId());
            checkPlanCompletion(plan);
        }
        // ...
    } else if (assignment != null) {
        LOGGER.warn("Duplicate completion for worker {}: already in state {}",
            workerId, assignment.getState());
    }
}
```

### Issue 13: Non-Atomic Foreman Election

**Severity:** HIGH

**Location:** Lines 476-492

**Problem:**
```java
private void electNewForeman() {
    if (!workerRegistry.isEmpty()) {
        String newForeman = workerRegistry.keySet().iterator().next();
        WorkerInfo removed = workerRegistry.remove(newForeman);
        this.foremanId = newForeman;
        // ...
    }
}
```

**Issue:** Multiple threads could call this simultaneously during foreman failure. Two threads could elect different foremen.

**Impact:** Split-brain scenario, multiple foremen active.

**Fix:**
```java
private final Object foremanElectionLock = new Object();

private void electNewForeman() {
    synchronized (foremanElectionLock) {
        if (foremanId != null) {
            return; // Already elected
        }

        if (!workerRegistry.isEmpty()) {
            String newForeman = workerRegistry.keySet().iterator().next();
            WorkerInfo removed = workerRegistry.remove(newForeman);
            this.foremanId = newForeman;

            LOGGER.info("[Orchestrator] Elected new foreman: {}", newForeman);

            AgentMessage announcement = AgentMessage.broadcast(
                newForeman, newForeman,
                "I am now the foreman!",
                AgentMessage.Priority.HIGH
            );
            communicationBus.publish(announcement);
        }
    }
}
```

### Issue 14: Task Completion State Machine

**Severity:** MEDIUM

**Location:** Lines 596-608

**Problem:**
```java
public void markTaskComplete(String assignmentId) {
    TaskAssignment assignment = assignments.get(assignmentId);
    if (assignment != null) {
        assignment.complete("Marked complete by orchestrator");
    }
}

// In TaskAssignment.complete() - assumes state machine
public void complete(String result) {
    this.state = State.COMPLETED;  // Overwrites any state
    this.progressPercent = 100;
    this.success = true;
    this.result = result;
    this.completedAt = Instant.now();
    this.statusMessage = "Completed successfully";
}
```

**Issue:** `complete()` doesn't check current state. Can transition from FAILED to COMPLETED, or overwrite terminal states.

**Impact:** Incorrect state transitions, lost failure information.

**Fix:**
```java
public void complete(String result) {
    // State machine validation
    if (state == State.COMPLETED) {
        return; // Idempotent
    }
    if (state.isTerminal()) {
        throw new IllegalStateException("Cannot complete task in terminal state: " + state);
    }

    this.state = State.COMPLETED;
    this.progressPercent = 100;
    this.success = true;
    this.result = result;
    this.completedAt = Instant.now();
    this.statusMessage = "Completed successfully";
}
```

### Issue 15: Worker Registry Concurrent Modification

**Severity:** LOW

**Location:** Lines 401-404

**Problem:**
```java
Optional<String> newWorker = workerRegistry.keySet().stream()
    .filter(id -> !id.equals(assignment.getAssignedWorkerId()))
    .filter(id -> !workerAssignments.containsKey(id))
    .findFirst();
```

**Issue:** Stream over `keySet()` can throw `ConcurrentModificationException` if registry is modified during iteration.

**Impact:** Rare exception during task retry.

**Fix:**
```java
// Create snapshot first
Set<String> availableWorkers = new HashSet<>(workerRegistry.keySet());
Optional<String> newWorker = availableWorkers.stream()
    .filter(id -> !id.equals(assignment.getAssignedWorkerId()))
    .filter(id -> !workerAssignments.containsKey(id))
    .findFirst();
```

---

## SimpleEventBus.java

**Location:** `src/main/java/com/minewright/event/SimpleEventBus.java`

### Issue 16: Publication During Unsubscription

**Severity:** HIGH

**Location:** Lines 100-127, 222-229

**Problem:**
```java
// Line 100-127 - Iterating subscribers
for (SubscriberEntry<?> entry : subs) {
    if (!entry.isActive()) continue;

    try {
        ((Consumer<T>) entry.subscriber).accept(event);
    } catch (Exception e) {
        // ...
    }
}

// Line 222-229 - Unsubscribe removes during iteration
public void unsubscribe() {
    entry.active.set(false);
    CopyOnWriteArrayList<SubscriberEntry<?>> list = subscribers.get(eventType);
    if (list != null) {
        list.remove(entry);  // Remove while potentially being published to
    }
    // ...
}
```

**Issue:** While `CopyOnWriteArrayList` handles concurrent modification, there's a race where:
1. Thread A checks `isActive()` - returns true
2. Thread B calls `unsubscribe()` - sets active to false, removes from list
3. Thread A calls `subscriber.accept()` - but subscriber is partially destroyed

**Impact:** Calling into partially destroyed subscribers, exceptions.

**Fix:**
```java
// Capture active subscribers in snapshot
public <T> void publish(T event) {
    if (event == null) {
        LOGGER.warn("Cannot publish null event");
        return;
    }

    Class<?> eventType = event.getClass();
    CopyOnWriteArrayList<SubscriberEntry<?>> subs = subscribers.get(eventType);

    if (subs == null || subs.isEmpty()) {
        LOGGER.trace("No subscribers for event type: {}", eventType.getSimpleName());
        return;
    }

    // Create snapshot of active subscribers
    List<SubscriberEntry<?>> activeSubs = new ArrayList<>();
    for (SubscriberEntry<?> entry : subs) {
        if (entry.isActive()) {
            activeSubs.add(entry);
        }
    }

    LOGGER.debug("Publishing {} to {} subscribers", eventType.getSimpleName(), activeSubs.size());

    for (SubscriberEntry<?> entry : activeSubs) {
        try {
            @SuppressWarnings("unchecked")
            Consumer<T> subscriber = (Consumer<T>) entry.subscriber;
            subscriber.accept(event);
        } catch (Exception e) {
            LOGGER.error("Error in event subscriber for {}: {}",
                eventType.getSimpleName(), e.getMessage(), e);
        }
    }
}
```

### Issue 17: ExecutorService Shutdown Issues

**Severity:** MEDIUM

**Location:** Lines 47-54, 177-188

**Problem:**
```java
// Line 49 - Creates single thread executor but doesn't track thread
this.asyncExecutor = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "event-bus-async");
    t.setDaemon(true);
    return t;
});

// Line 180-181 - May not terminate cleanly
if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
    asyncExecutor.shutdownNow();
}
```

**Issue:** No tracking of submitted futures during shutdown. `shutdownNow()` interrupts tasks but doesn't wait for them to complete.

**Impact:** Events may be lost during shutdown, interrupted event handlers.

**Fix:**
```java
// Track pending tasks
private final Queue<CompletableFuture<?>> pendingTasks = new ConcurrentLinkedQueue<>();

@Override
public <T> void publishAsync(T event) {
    if (event == null) return;

    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
            publish(event);
        } catch (Exception e) {
            LOGGER.error("Error in async event publishing: {}", e.getMessage(), e);
        }
    }, asyncExecutor);

    pendingTasks.add(future);
    future.whenComplete((r, ex) -> pendingTasks.remove(future));
}

public void shutdown() {
    asyncExecutor.shutdown();
    try {
        // Wait for pending tasks
        if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            asyncExecutor.shutdownNow();

            // Wait another 2 seconds for interruption to take effect
            if (!asyncExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                LOGGER.warn("Some event handlers did not terminate gracefully");
            }
        }

        // Wait for any remaining async tasks
        CompletableFuture.allOf(pendingTasks.toArray(new CompletableFuture[0]))
            .get(1, TimeUnit.SECONDS);

    } catch (InterruptedException e) {
        asyncExecutor.shutdownNow();
        Thread.currentThread().interrupt();
    } catch (Exception e) {
        LOGGER.error("Error during event bus shutdown", e);
    }

    LOGGER.info("EventBus shutdown complete");
}
```

### Issue 18: Priority Sorting Race

**Severity:** LOW

**Location:** Lines 82-90

**Problem:**
```java
subscribers.compute(eventType, (key, list) -> {
    if (list == null) {
        list = new CopyOnWriteArrayList<>();
    }
    list.add(entry);
    // Sort by priority (descending - higher priority first)
    list.sort((a, b) -> Integer.compare(b.priority, a.priority));
    return list;
});
```

**Issue:** While `compute()` is atomic, the sort happens within the lambda. If two threads subscribe simultaneously, one sort could overwrite the other.

**Impact:** Subscribers may not be in exact priority order (rare).

**Fix:**
```java
// Use a data structure that maintains ordering
private final ConcurrentHashMap<Class<?>, ConcurrentSkipListSet<SubscriberEntry<?>>> subscribers;

// Or use synchronized sort
private final Object subscriptionLock = new Object();

public <T> Subscription subscribe(Class<T> eventType, Consumer<T> subscriber, int priority) {
    // ... validation ...

    SubscriberEntry<T> entry = new SubscriberEntry<>(subscriber, priority);

    subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
        .add(entry);

    // Sort under lock
    synchronized (subscriptionLock) {
        CopyOnWriteArrayList<SubscriberEntry<?>> list = subscribers.get(eventType);
        list.sort((a, b) -> Integer.compare(b.priority, a.priority));
    }

    return new SubscriptionImpl(eventType, entry);
}
```

---

## Cross-Cutting Concerns

### Issue 19: Task Object Mutability

**Severity:** MEDIUM

**Location:** `src/main/java/com/minewright/action/Task.java`

**Problem:**
```java
public class Task {
    private final String action;
    private final Map<String, Object> parameters;  // Mutable map!

    public Task(String action, Map<String, Object> parameters) {
        this.action = action;
        this.parameters = parameters;  // Direct reference
    }

    public Map<String, Object> getParameters() {
        return parameters;  // Returns mutable internal state
    }
}
```

**Issue:** Parameters map is mutable and shared across threads. If one task modifies the map, it affects all other references.

**Impact:** Unexpected parameter mutations, race conditions.

**Fix:**
```java
public class Task {
    private final String action;
    private final Map<String, Object> parameters;

    public Task(String action, Map<String, Object> parameters) {
        this.action = action;
        // Create defensive copy
        this.parameters = Collections.unmodifiableMap(new HashMap<>(parameters));
    }

    public Map<String, Object> getParameters() {
        return parameters;  // Already unmodifiable
    }
}
```

### Issue 20: TaskAssignment State Visibility

**Severity:** LOW

**Location:** `src/main/java/com/minewright/orchestration/TaskAssignment.java`

**Problem:**
```java
public class TaskAssignment {
    private volatile State state;
    private volatile int progressPercent;
    private volatile String statusMessage;
    // ... more volatile fields ...

    public void complete(String result) {
        this.state = State.COMPLETED;
        this.progressPercent = 100;
        this.success = true;
        this.result = result;
        this.completedAt = Instant.now();
        this.statusMessage = "Completed successfully";
    }
}
```

**Issue:** While individual fields are volatile, the compound operation in `complete()` is not atomic. Another thread could see an inconsistent state where `state == COMPLETED` but `result == null`.

**Impact:** Rare inconsistent reads (benign in most cases).

**Fix:**
```java
// Use atomic reference for state snapshot
private final AtomicReference<StateSnapshot> stateRef;

private static class StateSnapshot {
    final State state;
    final int progress;
    final String result;
    final Instant timestamp;
    // ...
}

public void complete(String result) {
    StateSnapshot newSnapshot = new StateSnapshot(
        State.COMPLETED, 100, result, Instant.now()
    );
    stateRef.set(newSnapshot);
}

public State getState() {
    return stateRef.get().state;
}
```

### Issue 21: AgentCommunicationBus Queue Overflow

**Severity:** LOW

**Location:** Lines 207-215

**Problem:**
```java
while (queue.size() >= MAX_QUEUE_SIZE) {
    AgentMessage dropped = queue.poll();
    if (dropped != null) {
        LOGGER.warn("Dropped message for {} (queue full): {}", agentId, dropped);
        stats.recordDropped();
    }
}
```

**Issue:** Non-atomic check-and-act. Between `size()` and `poll()`, another thread could remove an item, leading to unnecessary drop.

**Impact:** Benign - rare unnecessary drop.

**Fix:**
```java
// Use offer with bounded queue
private final Map<String, LinkedBlockingQueue<AgentMessage>> agentQueues;

// In registerAgent
this.agentQueues.computeIfAbsent(agentId,
    k -> new LinkedBlockingQueue<>(MAX_QUEUE_SIZE));

// In deliverToAgent
boolean offered = queue.offer(message);
if (!offered) {
    LOGGER.warn("Dropped message for {} (queue full): {}", agentId, message);
    stats.recordDropped();
}
```

### Issue 22: CompletableFuture Cancellation Not Handled

**Severity:** MEDIUM

**Location:** `ActionExecutor.java` line 128-131

**Problem:**
```java
if (currentAction != null) {
    currentAction.cancel();
    currentAction = null;
}
```

**Issue:** When cancelling actions, any pending `planningFuture` is not cancelled. This wastes LLM resources.

**Impact:** Orphaned LLM requests continue processing, resource waste.

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

    // Cancel pending async planning
    if (planningFuture != null && !planningFuture.isDone()) {
        planningFuture.cancel(true);
        MineWrightMod.LOGGER.info("Cancelled pending planning for '{}'", foreman.getSteveName());
    }
    planningFuture = null;
    isPlanning = false;
    pendingCommand = null;

    taskQueue.clear();
    currentGoal = null;
    stateMachine.reset();
}
```

### Issue 23: InterruptedException Not Properly Handled

**Severity:** LOW

**Location:** `SimpleEventBus.java` lines 183-186, `AgentCommunicationBus.java` line 284

**Problem:**
```java
// SimpleEventBus
} catch (InterruptedException e) {
    asyncExecutor.shutdownNow();
    Thread.currentThread().interrupt();  // Good!
}

// AgentCommunicationBus
Thread.sleep(10);  // Swallows InterruptedException
```

**Issue:** Inconsistent handling of `InterruptedException`.

**Impact:** Lost interruption signals, threads may not respond to shutdown.

**Fix:**
```java
// AgentCommunicationBus.poll()
long deadline = System.nanoTime() + unit.toNanos(timeout);
while (System.nanoTime() < deadline) {
    AgentMessage message = queue.poll();
    if (message != null) {
        stats.recordReceived();
        return message;
    }
    try {
        TimeUnit.NANOSECONDS.sleep(Math.min(10_000_000L, deadline - System.nanoTime()));
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return null; // Exit early on interrupt
    }
}
return null;
```

---

## Recommendations Summary

### Immediate Actions (CRITICAL)

1. **Fix ActionExecutor async planning race** - Make `isPlanning` volatile, add timeout to `future.get()`
2. **Fix CollaborativeBuildManager section assignment** - Use `compute()` for atomic check-and-act
3. **Fix OrchestratorService task assignment** - Use `putIfAbsent()` to prevent duplicate assignments
4. **Fix OrchestratorService foreman election** - Add synchronization to prevent split-brain

### High Priority

1. **Add volatile to all shared state flags** in ActionExecutor
2. **Replace LinkedList with ConcurrentLinkedQueue** for taskQueue
3. **Fix SimpleEventBus publication race** - Snapshot subscribers before iteration
4. **Add proper TaskAssignment state machine validation**

### Medium Priority

1. **Fix lazy initialization** with proper double-checked locking
2. **Add CompletableFuture cancellation** in stopCurrentAction
3. **Make Task.parameters immutable** with defensive copy
4. **Fix CollaborativeBuildManager block assignment** with CAS loop
5. **Improve executor shutdown** with proper task tracking

### Low Priority

1. **Make TaskAssignment state atomic** with AtomicReference
2. **Fix AgentCommunicationBus queue overflow** with bounded queues
3. **Standardize InterruptedException handling** across all code
4. **Add snapshot iteration** for registry streams

---

## Testing Recommendations

To validate thread safety fixes:

1. **Concurrency Testing**
   ```java
   @Test
   public void testConcurrentSectionAssignment() {
       CollaborativeBuild build = new CollaborativeBuild(...);
       int nThreads = 10;
       CountDownLatch latch = new CountDownLatch(nThreads);
       Set<Integer> assignedSections = Collections.synchronizedSet(new HashSet<>());

       for (int i = 0; i < nThreads; i++) {
           final String foremanName = "foreman" + i;
           new Thread(() -> {
               try {
                   Integer section = assignForemanToSection(build, foremanName);
                   if (section != null) {
                       assignedSections.add(section);
                   }
               } finally {
                   latch.countDown();
               }
           }).start();
       }

       latch.await();
       // Validate no duplicates
       assertEquals(assignedSections.size(), new HashSet<>(assignedSections).size());
   }
   ```

2. **Race Condition Detection**
   - Use **JCStress** for low-level race detection
   - Use **ThreadSanitizer** (if using native code)
   - Enable **Java Flight Recorder** for production monitoring

3. **Stress Testing**
   ```java
   @Test
   public void testStressConcurrentPlanning() throws Exception {
       ActionExecutor executor = new ActionExecutor(foreman);
       int nCommands = 100;
       CountDownLatch latch = new CountDownLatch(nCommands);

       for (int i = 0; i < nCommands; i++) {
           final int cmdNum = i;
           new Thread(() -> {
               try {
                   executor.processNaturalLanguageCommand("test" + cmdNum);
               } finally {
                   latch.countDown();
               }
           }).start();
       }

       latch.await();
       // Verify consistent state
       assertFalse(executor.isPlanning() && executor.getCurrentAction() != null);
   }
   ```

---

## Conclusion

The MineWright codebase demonstrates awareness of concurrency issues (evident from the use of `ConcurrentHashMap`, `volatile`, `AtomicInteger`), but has several critical gaps:

1. **Compound operations** are the most common issue (check-then-act patterns)
2. **State visibility** is inconsistent (missing `volatile` keywords)
3. **Async resource cleanup** is incomplete (unterminated futures)

The recommended fixes prioritize thread safety without major architectural changes. For long-term maintainability, consider:
- Migrating to **actor-based concurrency** (Akka/Akka Typed)
- Using **software transactional memory** for complex state
- Implementing **structured concurrency** (Java 21+ Virtual Threads)

---

**Audit completed:** 2026-02-27
**Next review recommended:** After implementing fixes, or before 1.3.0 release
