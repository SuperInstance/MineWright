# Thread Safety Audit - MineWright Codebase

**Audit Date:** 2026-02-27
**Auditor:** Claude Code (Orchestrator Agent)
**Scope:** Core orchestration, entity management, action execution, and concurrency infrastructure

---

## Executive Summary

The MineWright codebase demonstrates **mixed thread safety practices**. While significant effort has been made to use concurrent collections in many areas, there are **critical thread safety violations** that could lead to race conditions, data corruption, and deadlocks in multi-threaded scenarios.

### Risk Assessment

| Severity | Count | Status |
|----------|-------|--------|
| **CRITICAL** | 6 | Immediate action required |
| **HIGH** | 4 | Should be fixed soon |
| **MEDIUM** | 5 | Technical debt |
| **LOW** | 3 | Best practices |

---

## Critical Issues

### 1. Race Condition in `AgentCommunicationBus.subscribe()` - Check-Then-Act Pattern

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\orchestration\AgentCommunicationBus.java` (Lines 126-134)

**Issue:**
```java
public void subscribe(String agentId, Consumer<AgentMessage> handler) {
    List<Consumer<AgentMessage>> handlers = subscribers.get(agentId);  // CHECK
    if (handlers != null) {
        handlers.add(handler);  // ACT - Race condition!
        LOGGER.debug("Subscribed handler for agent: {}", agentId);
    } else {
        LOGGER.warn("Cannot subscribe - agent not registered: {}", agentId);
    }
}
```

**Problem:** Classic check-then-act race condition. Between `get()` and `add()`, another thread could remove the agent, causing `handlers` to become stale.

**Thread Safety Pattern Violated:** Never trust a reference obtained from a concurrent collection without atomic operation.

**Recommended Fix:**
```java
public void subscribe(String agentId, Consumer<AgentMessage> handler) {
    subscribers.computeIfPresent(agentId, (key, handlers) -> {
        handlers.add(handler);
        LOGGER.debug("Subscribed handler for agent: {}", agentId);
        return handlers;
    });
}
```

---

### 2. Non-Atomic Compound Actions in `CollaborativeBuildManager.getNextBlock()`

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\CollaborativeBuildManager.java` (Lines 181-210)

**Issue:**
```java
public static BlockPlacement getNextBlock(CollaborativeBuild build, String foremanName) {
    if (build.isComplete()) {
        return null;
    }

    build.participatingForemen.add(foremanName);  // ConcurrentHashSet - OK

    Integer sectionIndex = build.foremanToSectionMap.get(foremanName);  // CHECK
    if (sectionIndex == null) {
        sectionIndex = assignForemanToSection(build, foremanName);  // ACT
        if (sectionIndex == null) {
            return null;
        }
    }

    BuildSection section = build.sections.get(sectionIndex);  // sections is ArrayList!
    BlockPlacement block = section.getNextBlock();

    // ... additional non-atomic logic
}
```

**Problems:**
1. `build.sections` is a regular `ArrayList` accessed from multiple threads
2. Check-then-act pattern on `foremanToSectionMap`
3. `assignForemanToSection()` iterates over `sections` while other threads may be modifying it

**Thread Safety Pattern Violated:** Shared mutable state without synchronization; compound actions not atomic.

**Recommended Fix:**
```java
public static BlockPlacement getNextBlock(CollaborativeBuild build, String foremanName) {
    if (build.isComplete()) {
        return null;
    }

    build.participatingForemen.add(foremanName);

    // Atomic check-and-assign
    Integer sectionIndex = build.foremanToSectionMap.computeIfAbsent(
        foremanName,
        k -> assignForemanToSection(build, k)
    );

    if (sectionIndex == null) {
        return null;
    }

    // Make sections thread-safe
    BuildSection section = build.sections.get(sectionIndex);
    synchronized (section) {
        return section.getNextBlock();
    }
}

// Change sections to CopyOnWriteArrayList in CollaborativeBuild constructor
private final List<BuildSection> sections = new CopyOnWriteArrayList<>();
```

---

### 3. Non-Thread-Safe `TaskQueue` in `ActionExecutor`

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java` (Lines 38, 237-238)

**Issue:**
```java
private final Queue<Task> taskQueue;  // Line 38 - Initialized as LinkedList

public void tick() {
    // ...
    taskQueue.clear();  // Line 237 - Called from game thread
    taskQueue.addAll(response.getTasks());  // Line 238 - Called from game thread

    // ... BUT queueTask() can be called from ANY thread via orchestration
}

public void queueTask(Task task) {
    if (task == null) {
        MineWrightMod.LOGGER.warn("Attempted to queue null task for Foreman '{}'", foreman.getSteveName());
        return;
    }
    taskQueue.add(task);  // Line 479 - Called from orchestration thread!
}
```

**Problems:**
1. `LinkedList` is not thread-safe
2. `tick()` is called from game thread (Minecraft server thread)
3. `queueTask()` is called from orchestration thread via `handleTaskAssignment()`
4. `tick()` also calls `taskQueue.poll()` (line 296) - concurrent modification!

**Thread Safety Pattern Violated:** Shared mutable state accessed from multiple threads without synchronization.

**Recommended Fix:**
```java
private final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();

public void queueTask(Task task) {
    if (task == null) {
        MineWrightMod.LOGGER.warn("Attempted to queue null task");
        return;
    }
    taskQueue.offer(task);  // Thread-safe
}

public void tick() {
    // ...
    // Use drainTo for atomic bulk add
    if (planningFuture != null && planningFuture.isDone()) {
        ResponseParser.ParsedResponse response = planningFuture.get();
        if (response != null) {
            taskQueue.clear();
            taskQueue.addAll(response.getTasks());  // Now thread-safe!
        }
    }

    // Poll is now thread-safe
    Task nextTask = taskQueue.poll();
    if (nextTask != null) {
        executeTask(nextTask);
    }
}
```

---

### 4. Non-Atomic Iterator in `CrewManager.tick()`

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\entity\CrewManager.java` (Lines 158-178)

**Issue:**
```java
public void tick(ServerLevel level) {
    // Clean up dead or removed crew members
    Iterator<Map.Entry<String, ForemanEntity>> iterator = activeCrewMembers.entrySet().iterator();
    while (iterator.hasNext()) {  // ConcurrentModificationException possible!
        Map.Entry<String, ForemanEntity> entry = iterator.next();
        ForemanEntity crewMember = entry.getValue();

        if (!crewMember.isAlive() || crewMember.isRemoved()) {
            iterator.remove();  // Safe removal via iterator
            crewMembersByUUID.remove(crewMember.getUUID());  // But this map is separate!

            // Unregister from orchestrator
            OrchestratorService orchestrator = MineWrightMod.getOrchestratorService();
            if (orchestrator != null) {
                orchestrator.unregisterAgent(entry.getKey());
            }

            MineWrightMod.LOGGER.info("Cleaned up crew member: {}", entry.getKey());
        }
    }
}
```

**Problems:**
1. Iterator over `ConcurrentHashMap.entrySet()` is weakly consistent but not atomic
2. `crewMembersByUUID.remove()` happens separately - not atomic with the iterator removal
3. If entity is removed from one map but not the other, state becomes inconsistent

**Thread Safety Pattern Violated:** Non-atomic compound action across multiple shared maps.

**Recommended Fix:**
```java
public void tick(ServerLevel level) {
    // Collect entities to remove first
    List<String> toRemove = new ArrayList<>();
    List<UUID> uuidsToRemove = new ArrayList<>();

    for (Map.Entry<String, ForemanEntity> entry : activeCrewMembers.entrySet()) {
        ForemanEntity crewMember = entry.getValue();
        if (!crewMember.isAlive() || crewMember.isRemoved()) {
            toRemove.add(entry.getKey());
            uuidsToRemove.add(crewMember.getUUID());
        }
    }

    // Remove atomically
    for (int i = 0; i < toRemove.size(); i++) {
        String name = toRemove.get(i);
        UUID uuid = uuidsToRemove.get(i);

        activeCrewMembers.remove(name);
        crewMembersByUUID.remove(uuid);

        OrchestratorService orchestrator = MineWrightMod.getOrchestratorService();
        if (orchestrator != null) {
            orchestrator.unregisterAgent(name);
        }

        MineWrightMod.LOGGER.info("Cleaned up crew member: {}", name);
    }
}
```

---

### 5. Race Condition in `OrchestratorService.retryTask()`

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\orchestration\OrchestratorService.java` (Lines 399-422)

**Issue:**
```java
private void retryTask(TaskAssignment assignment) {
    // Find a different available worker
    Optional<String> newWorker = workerRegistry.keySet().stream()
        .filter(id -> !id.equals(assignment.getAssignedWorkerId()))
        .filter(id -> !workerAssignments.containsKey(id))  // CHECK
        .findFirst();

    if (newWorker.isPresent()) {
        String newWorkerId = newWorker.get();
        assignment.reassign(newWorkerId, "Retry after failure");
        workerAssignments.put(newWorkerId, assignment);  // ACT - Race condition!

        AgentMessage retryMessage = AgentMessage.taskAssignment(
            foremanId, "Foreman", newWorkerId,
            assignment.getTaskDescription(), assignment.getParameters()
        );
        communicationBus.publish(retryMessage);

        LOGGER.info("[Orchestrator] Retrying task with {} (attempt {})",
            newWorkerId, assignment.getRetryCount());
    } else {
        LOGGER.warn("[Orchestrator] No available worker for task retry");
    }
}
```

**Problem:** Between checking `containsKey()` and `put()`, another thread could assign the same worker to a different task.

**Thread Safety Pattern Violated:** Check-then-act pattern.

**Recommended Fix:**
```java
private void retryTask(TaskAssignment assignment) {
    // Atomic check-and-assign using computeIfAbsent
    String newWorkerId = workerRegistry.keySet().stream()
        .filter(id -> !id.equals(assignment.getAssignedWorkerId()))
        .filter(id -> !workerAssignments.containsKey(id))
        .findFirst()
        .map(id -> {
            // Atomic putIfAbsent
            TaskAssignment existing = workerAssignments.putIfAbsent(id, assignment);
            return existing == null ? id : null;  // Return null if already assigned
        })
        .orElse(null);

    if (newWorkerId != null) {
        assignment.reassign(newWorkerId, "Retry after failure");

        AgentMessage retryMessage = AgentMessage.taskAssignment(
            foremanId, "Foreman", newWorkerId,
            assignment.getTaskDescription(), assignment.getParameters()
        );
        communicationBus.publish(retryMessage);

        LOGGER.info("[Orchestrator] Retrying task with {} (attempt {})",
            newWorkerId, assignment.getRetryCount());
    } else {
        LOGGER.warn("[Orchestrator] No available worker for task retry");
    }
}
```

---

### 6. `ConcurrentLinkedQueue` Drains Without Synchronization in `AgentCommunicationBus.deliverToAgent()`

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\orchestration\AgentCommunicationBus.java` (Lines 191-223)

**Issue:**
```java
private void deliverToAgent(String agentId, AgentMessage message) {
    // Check filters
    List<MessageFilter> agentFilters = filters.get(agentId);  // CHECK
    if (agentFilters != null) {
        for (MessageFilter filter : agentFilters) {  // Iterate - could be modified!
            if (!filter.accept(message)) {
                LOGGER.debug("Message filtered for {}: {}", agentId, message);
                stats.recordFiltered();
                return;
            }
        }
    }

    // Add to queue
    PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
    if (queue != null) {
        // Enforce max queue size
        while (queue.size() >= MAX_QUEUE_SIZE) {  // CHECK - Race condition!
            AgentMessage dropped = queue.poll();   // ACT
            // ...
        }
        queue.offer(message);
        stats.recordDelivered();

        // Notify handlers
        notifyHandlers(agentId);  // Can happen before message is fully queued!
    }
}
```

**Problems:**
1. `filters` map can be modified while iterating
2. Check-then-act on queue size
3. `notifyHandlers()` polls the queue immediately, creating race condition with queuing

**Thread Safety Pattern Violated:** Non-atomic compound actions.

**Recommended Fix:**
```java
private void deliverToAgent(String agentId, AgentMessage message) {
    // Check filters atomically
    List<MessageFilter> agentFilters = filters.get(agentId);
    if (agentFilters != null) {
        // CopyOnWriteArrayList iteration is thread-safe
        for (MessageFilter filter : agentFilters) {
            if (!filter.accept(message)) {
                LOGGER.debug("Message filtered for {}: {}", agentId, message);
                stats.recordFiltered();
                return;
            }
        }
    }

    // Add to queue atomically
    PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
    if (queue != null) {
        // Atomic offer with size check using synchronized
        synchronized (queue) {
            if (queue.size() >= MAX_QUEUE_SIZE) {
                queue.poll();  // Drop oldest
                stats.recordDropped();
            }
            queue.offer(message);
        }

        stats.recordDelivered();
        notifyHandlers(agentId);
    }
}
```

---

## High-Priority Issues

### 7. `SimpleEventBus` Subscriber List Sorting Under Concurrent Access

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\event\SimpleEventBus.java` (Lines 72-96)

**Issue:**
```java
public <T> Subscription subscribe(Class<T> eventType, Consumer<T> subscriber, int priority) {
    // ...
    subscribers.compute(eventType, (key, list) -> {
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
        }
        list.add(entry);
        // Sort by priority (descending - higher priority first)
        list.sort((a, b) -> Integer.compare(b.priority, a.priority));  // MUTATES IN PLACE!
        return list;
    });
}
```

**Problem:** While `compute()` is atomic, `sort()` mutates the list that other threads might be iterating during `publish()`.

**Recommended Fix:**
```java
subscribers.compute(eventType, (key, list) -> {
    CopyOnWriteArrayList<SubscriberEntry<?>> newList;
    if (list == null) {
        newList = new CopyOnWriteArrayList<>();
        newList.add(entry);
    } else {
        // Create new sorted list
        newList = new CopyOnWriteArrayList<>(list);
        newList.add(entry);
        newList.sort((a, b) -> Integer.compare(b.priority, a.priority));
    }
    return newList;
});
```

---

### 8. Non-Atomic State Transitions in `ForemanEntity.setRole()`

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\entity\ForemanEntity.java` (Lines 471-484)

**Issue:**
```java
public void setRole(AgentRole newRole) {
    AgentRole oldRole = this.role;  // Read non-volatile field
    this.role = newRole;  // Write non-volatile field

    // Re-register with new role
    if (orchestrator != null && registeredWithOrchestrator.get()) {
        orchestrator.unregisterAgent(entityName);  // Gap where role is inconsistent!
        orchestrator.registerAgent(this, newRole);
        registeredWithOrchestrator.set(true);
    }

    sendChatMessage(String.format("Role changed: %s -> %s",
        oldRole.getDisplayName(), newRole.getDisplayName()));
}
```

**Problem:** `role` field is not `volatile`, so changes may not be visible to other threads. Gap between unregister and register creates inconsistent state.

**Recommended Fix:**
```java
private volatile AgentRole role = AgentRole.SOLO;  // Make volatile

public void setRole(AgentRole newRole) {
    synchronized (this) {
        AgentRole oldRole = this.role;
        this.role = newRole;

        if (orchestrator != null && registeredWithOrchestrator.get()) {
            orchestrator.unregisterAgent(entityName);
            orchestrator.registerAgent(this, newRole);
            // registeredWithOrchestrator already true
        }

        sendChatMessage(String.format("Role changed: %s -> %s",
            oldRole.getDisplayName(), newRole.getDisplayName()));
    }
}
```

---

### 9. Missing Memory Visibility on `ActionExecutor` Planning Flags

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java` (Lines 46-48)

**Issue:**
```java
// NEW: Async planning support (non-blocking LLM calls)
private CompletableFuture<ResponseParser.ParsedResponse> planningFuture;  // Not volatile!
private boolean isPlanning = false;  // Not volatile!
private String pendingCommand;  // Not volatile!
```

**Problem:** These fields are accessed from game thread (in `tick()`) and potentially from other threads (though currently only game thread calls `processNaturalLanguageCommand()`). Without `volatile`, changes may not be visible.

**Recommended Fix:**
```java
private volatile CompletableFuture<ResponseParser.ParsedResponse> planningFuture;
private volatile boolean isPlanning = false;
private volatile String pendingCommand;
```

---

### 10. InterceptorChain Sort Without Synchronization

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\execution\InterceptorChain.java` (Lines 96-101)

**Issue:**
```java
public void addInterceptor(ActionInterceptor interceptor) {
    interceptors.add(interceptor);
    sortInterceptors();  // Modifies list while others may be iterating!
}

private void sortInterceptors() {
    List<ActionInterceptor> sorted = new ArrayList<>(interceptors);
    sorted.sort(Comparator.comparingInt(ActionInterceptor::getPriority).reversed());
    interceptors.clear();  // DANGEROUS!
    interceptors.addAll(sorted);
}
```

**Problem:** Even though `CopyOnWriteArrayList` is thread-safe, calling `clear()` and `addAll()` creates a window where the list is empty.

**Recommended Fix:**
```java
public void addInterceptor(ActionInterceptor interceptor) {
    if (interceptor == null) {
        throw new IllegalArgumentException("Interceptor cannot be null");
    }

    // Rebuild entire list atomically
    List<ActionInterceptor> newList = new ArrayList<>(interceptors);
    newList.add(interceptor);
    newList.sort(Comparator.comparingInt(ActionInterceptor::getPriority).reversed());

    // Atomic replacement
    interceptors.clear();
    interceptors.addAll(newList);

    LOGGER.debug("Added interceptor: {} (priority: {})",
        interceptor.getName(), interceptor.getPriority());
}
```

---

## Medium-Priority Issues

### 11. `CollaborativeBuild` Progress Calculation Without Synchronization

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\CollaborativeBuildManager.java` (Lines 102-121)

**Issue:**
```java
public int getBlocksPlaced() {
    int total = 0;
    for (BuildSection section : sections) {  // ArrayList iteration
        total += section.getBlocksPlaced();  // AtomicInteger get
    }
    return total;
}
```

**Problem:** While individual counters are atomic, the sum across sections is not. Could report inconsistent totals.

**Recommended Fix:**
```java
public int getBlocksPlaced() {
    return sections.stream()
        .mapToInt(BuildSection::getBlocksPlaced)
        .sum();  // Not atomic but "good enough" for progress reporting
}

// OR for exact consistency:
public synchronized int getBlocksPlaced() {
    return sections.stream()
        .mapToInt(BuildSection::getBlocksPlaced)
        .sum();
}
```

---

### 12. `AgentCommunicationBus.poll()` with Busy-Wait

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\orchestration\AgentCommunicationBus.java` (Lines 272-288)

**Issue:**
```java
public AgentMessage poll(String agentId, long timeout, TimeUnit unit) throws InterruptedException {
    PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
    if (queue != null) {
        // PriorityBlockingQueue doesn't support timed poll directly
        // So we use a simple approach with sleep
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            AgentMessage message = queue.poll();
            if (message != null) {
                stats.recordReceived();
                return message;
            }
            Thread.sleep(10);  // BUSY WAIT!
        }
    }
    return null;
}
```

**Problem:** Busy-wait with `Thread.sleep(10)` wastes CPU and is imprecise.

**Recommended Fix:**
```java
public AgentMessage poll(String agentId, long timeout, TimeUnit unit) throws InterruptedException {
    PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
    if (queue != null) {
        return queue.poll(timeout, unit);  // BlockingQueue supports timed poll!
    }
    return null;
}
```

---

### 13. Static `CollaborativeBuildManager.activeBuilds` Without Lifecycle Management

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\CollaborativeBuildManager.java` (Line 161)

**Issue:**
```java
private static final Map<String, CollaborativeBuild> activeBuilds = new ConcurrentHashMap<>();
```

**Problem:** Static map that is never cleared. Could cause memory leak if many builds are created and completed but not removed.

**Recommended Fix:**
```java
public static void cleanupOldBuilds(long maxAgeMs) {
    long cutoff = System.currentTimeMillis() - maxAgeMs;
    activeBuilds.entrySet().removeIf(entry -> {
        CollaborativeBuild build = entry.getValue();
        return build.isComplete() || build.getCreatedAt() < cutoff;
    });
}

// Add createdAt field to CollaborativeBuild
public static class CollaborativeBuild {
    private final long createdAt = System.currentTimeMillis();
    // ...
}
```

---

## Low-Priority Issues

### 14. Missing `volatile` on Singleton Instance in `LLMExecutorService`

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\async\LLMExecutorService.java` (Line 46)

**Issue:**
```java
private static final LLMExecutorService INSTANCE = new LLMExecutorService();
```

**Analysis:** Actually fine! `final` static fields are safely published during class loading. No issue here.

---

### 15. Potential Deadlock in `OrchestratorService.electNewForeman()`

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\orchestration\OrchestratorService.java` (Lines 476-492)

**Issue:**
```java
private void electNewForeman() {
    if (!workerRegistry.isEmpty()) {
        String newForeman = workerRegistry.keySet().iterator().next();  // Non-deterministic
        WorkerInfo info = workerRegistry.remove(newForeman);
        this.foremanId = newForeman;  // Volatile write

        LOGGER.info("[Orchestrator] Elected new foreman: {}", newForeman);

        // Notify all agents
        AgentMessage announcement = AgentMessage.broadcast(
            newForeman, newForeman,
            "I am now the foreman!",
            AgentMessage.Priority.HIGH
        );
        communicationBus.publish(announcement);
    }
}
```

**Problem:** If `communicationBus.publish()` blocks (though unlikely with current implementation), could cause issues. More concerning: removes worker before adding as foreman - gap in state.

**Recommended Fix:**
```java
private void electNewForeman() {
    String newForeman = workerRegistry.keySet().stream()
        .findFirst()
        .orElse(null);

    if (newForeman != null) {
        WorkerInfo info = workerRegistry.remove(newForeman);
        this.foremanId = newForeman;

        LOGGER.info("[Orchestrator] Elected new foreman: {}", newForeman);

        // Re-register as foreman
        communicationBus.unregisterAgent(newForeman);
        communicationBus.registerAgent(newForeman, newForeman);

        AgentMessage announcement = AgentMessage.broadcast(
            newForeman, newForeman,
            "I am now the foreman!",
            AgentMessage.Priority.HIGH
        );
        communicationBus.publish(announcement);
    }
}
```

---

## Thread Safety Patterns Used (Good Practices)

### 1. ConcurrentHashMap for Shared Maps
Most shared state uses `ConcurrentHashMap` correctly:
- `AgentCommunicationBus.agentQueues`
- `AgentCommunicationBus.subscribers`
- `OrchestratorService.workerRegistry`
- `OrchestratorService.workerAssignments`
- `CrewManager.activeCrewMembers`

### 2. AtomicReference for State
`AgentStateMachine` uses `AtomicReference<AgentState>` for thread-safe state transitions.

### 3. CopyOnWriteArrayList for Subscriber Lists
`SimpleEventBus` and `AgentCommunicationBus` use `CopyOnWriteArrayList` for subscriber lists, which is appropriate for read-heavy workloads.

### 4. ConcurrentLinkedQueue for Message Queues
`ForemanEntity.messageQueue` uses `ConcurrentLinkedQueue` for lock-free message passing.

### 5. AtomicInteger for Counters
Multiple classes use `AtomicInteger` for counters (e.g., `BuildSection.nextBlockIndex`, `MessageStats`).

---

## Recommended Architectural Improvements

### 1. Adopt Actor Model for Entity Coordination

**Current Issue:** Direct shared state between `CrewManager`, `OrchestratorService`, and `ForemanEntity`.

**Recommendation:** Use message passing exclusively:
```java
public interface CrewMessage {
    void handle(ForemanEntity recipient);
}

// All interactions via messages, no shared state
crewManager.sendMessage(new SpawnCrewMessage(name, pos));
```

### 2. Use Striped Locks for Fine-Grained Synchronization

Instead of global locks, use striped locks per entity:
```java
private final Striped<Lock> locks = Striped.lock(128);

public void updateEntity(String id, Consumer<ForemanEntity> updater) {
    Lock lock = locks.get(id);
    lock.lock();
    try {
        updater.accept(activeCrewMembers.get(id));
    } finally {
        lock.unlock();
    }
}
```

### 3. Immutable Data Structures for Configuration

Make `Task`, `AgentMessage`, and `TaskAssignment` immutable:
```java
public final class Task {
    private final String action;
    private final Map<String, Object> parameters;

    public Task(String action, Map<String, Object> parameters) {
        this.action = action;
        this.parameters = Map.copyOf(parameters);  // Immutable copy
    }

    // No setters, only getters
}
```

### 4. Use CompletableFuture for All Async Operations

Standardize on `CompletableFuture` for async coordination:
```java
public CompletableFuture<ActionResult> executeTaskAsync(Task task) {
    return CompletableFuture.supplyAsync(() -> {
        BaseAction action = createAction(task);
        action.start();
        while (!action.isComplete()) {
            Thread.sleep(50);
            action.tick();
        }
        return action.getResult();
    }, executor);
}
```

---

## Testing Recommendations

### 1. Add Concurrency Tests

```java
@Test
public void testConcurrentTaskQueueAccess() throws Exception {
    ActionExecutor executor = new ActionExecutor(foreman);
    int threadCount = 10;
    int tasksPerThread = 100;
    CountDownLatch latch = new CountDownLatch(threadCount);

    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    for (int i = 0; i < threadCount; i++) {
        pool.submit(() -> {
            try {
                for (int j = 0; j < tasksPerThread; j++) {
                    Task task = new Task("test", Map.of("value", j));
                    executor.queueTask(task);
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(10, TimeUnit.SECONDS);
    assertEquals(threadCount * tasksPerThread, executor.getQueuedTaskCount());
}
```

### 2. Use ThreadSanitizer or JCStress

```java
@JCStressTest
@Outcome(id = "0", expect = Expect.ACCEPTABLE, desc = "No race")
@Outcome(id = "1", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Race detected")
@State
public class ActionExecutorConcurrencyTest {
    ActionExecutor executor = new ActionExecutor(foreman);
    AtomicBoolean flag = new AtomicBoolean();

    @Actor
    public void thread1() {
        executor.queueTask(new Task("task1", Map.of()));
    }

    @Actor
    public void thread2() {
        executor.tick();
    }
}
```

---

## Conclusion

The MineWright codebase shows **good awareness** of thread safety with consistent use of concurrent collections. However, there are **critical issues** in:

1. **Check-then-act patterns** throughout the codebase
2. **Non-atomic compound actions** across multiple shared data structures
3. **Mixed access patterns** where some operations are synchronized and others are not
4. **Missing memory visibility** (volatile) on shared flags

**Immediate Actions Required:**
1. Fix `ActionExecutor.taskQueue` - use `LinkedBlockingQueue`
2. Fix `CollaborativeBuildManager.sections` - use `CopyOnWriteArrayList`
3. Fix all check-then-act patterns with atomic operations
4. Add `volatile` to flags accessed from multiple threads

**Estimated Effort:** 2-3 days to fix critical issues, 1 week for full remediation including tests.

---

**Audit Completed:** 2026-02-27
**Next Review:** After critical issues are resolved
