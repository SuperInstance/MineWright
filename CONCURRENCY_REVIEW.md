# Thread Safety and Concurrency Review
## MineWright Minecraft Mod

**Review Date:** 2026-02-27
**Reviewer:** Concurrency Analysis Specialist
**Scope:** Async LLM infrastructure, orchestration, action execution, and memory systems

---

## Executive Summary

This review identified **23 concurrency issues** across the codebase:
- **6 RACE** - Race conditions in shared state access
- **4 DEADLOCK** - Potential deadlock scenarios
- **3 ESCAPE** - Object escape and publication issues
- **7 BLOCKING** - Blocking operations that shouldn't block
- **3 SAFE** - Correctly synchronized code (commendations)

### Critical Issues Requiring Immediate Attention

1. **RACE: ActionExecutor compound operations on shared state** (Lines 252-293, 388-435)
2. **ESCAPE: AsyncOpenAIClient static ScheduledExecutorService** (Lines 68-89)
3. **RACE: LLMCache LRU eviction not atomic** (Lines 82-107)
4. **BLOCKING: ActionExecutor.tick() has 60-second blocking wait** (Line 395)
5. **DEADLOCK: OrchestratorService nested map operations** (Lines 489-531)

---

## Detailed Findings

### RACE: Race Conditions

#### 1. RACE: ActionExecutor compound check-then-act operations

**Location:** `ActionExecutor.java:249-293`
**Severity:** HIGH

**Problem:**
```java
// Line 252-256
if (isPlanning) {
    MineWrightMod.LOGGER.warn("Foreman '{}' is already planning...",);
    return;
}
// Line 272-273
this.pendingCommand = command;
this.isPlanning = true;
```

The check of `isPlanning` and the subsequent state update are **not atomic**. Between checking the flag and setting it, another thread could also pass the check, leading to:
- Multiple concurrent LLM calls
- Lost command updates
- Inconsistent state

**Impact:** In multi-threaded scenarios (e.g., orchestration assignments), multiple commands could be processed simultaneously.

**Fix:**
```java
// Use AtomicInteger with compareAndSet for atomic state transition
private final AtomicInteger planningState = new AtomicInteger(0); // 0=idle, 1=planning

public void processNaturalLanguageCommand(String command) {
    // Atomic transition: idle -> planning
    if (!planningState.compareAndSet(0, 1)) {
        MineWrightMod.LOGGER.warn("Foreman '{}' is already planning", foreman.getEntityName());
        return;
    }

    try {
        this.pendingCommand = command;
        // ... rest of method
    } catch (Exception e) {
        planningState.set(0); // Reset on error
        throw e;
    }
}
```

---

#### 2. RACE: ActionExecutor planning future null check

**Location:** `ActionExecutor.java:392-435`
**Severity:** MEDIUM

**Problem:**
```java
// Line 392
if (isPlanning && planningFuture != null && planningFuture.isDone()) {
    try {
        // Line 395: BLOCKING CALL UP TO 60 SECONDS
        ResponseParser.ParsedResponse response = planningFuture.get(60, TimeUnit.SECONDS);
```

While `planningFuture` is volatile, the check `isDone()` and subsequent `get()` are not atomic. The future could complete between the check and get, though this is less critical since `get()` handles completed futures.

**More Critical:** The **blocking `get(60, TimeUnit.SECONDS)`** call on the Minecraft server thread can freeze the server for up to 60 seconds.

**Fix:**
```java
// Use non-blocking polling with timeout
if (isPlanning && planningFuture != null && planningFuture.isDone()) {
    try {
        // Use getNow() instead of get() - returns immediately
        ResponseParser.ParsedResponse response = planningFuture.getNow(null);

        if (response == null) {
            // Future completed exceptionally
            MineWrightMod.LOGGER.warn("Planning future completed with exception");
            stateMachine.forceTransition(AgentState.IDLE, "planning failed");
        } else {
            // Process response...
        }
    } finally {
        isPlanning = false;
        planningFuture = null;
    }
}
```

---

#### 3. RACE: LLMCache LRU eviction is not atomic

**Location:** `LLMCache.java:53-76, 82-107`
**Severity:** HIGH

**Problem:**
```java
// Line 58-65: Cache GET
if (entry != null) {
    if (System.currentTimeMillis() - entry.timestamp < TTL_MS) {
        hitCount.incrementAndGet();
        accessOrder.remove(key);      // NOT ATOMIC with addLast
        accessOrder.addLast(key);
        return Optional.of(entry.response);
    }
}

// Line 86-93: Cache PUT
while (cache.size() >= MAX_CACHE_SIZE) {
    evictOldest();  // Multiple threads can evict simultaneously
}
cache.put(key, new CacheEntry(cachedResponse));
accessOrder.addLast(key);
```

**Race conditions:**
1. **LRU list corruption:** `remove()` + `addLast()` is not atomic. Two threads can cause the same key to appear twice or be lost.
2. **Double eviction:** Multiple threads can pass the `while` check and evict multiple entries when only one should be removed.
3. **Lost updates:** Between checking timestamp and updating access order, another thread can evict the entry.

**Impact:** Cache corruption, incorrect LRU ordering, potential memory leaks.

**Fix:**
```java
private final ConcurrentHashMap<Integer, CacheEntry> cache;
private final ConcurrentHashMap<Integer, Long> accessTimestamps; // Track access times
private final AtomicLong counter = new AtomicLong(0); // For LRU ordering

public Optional<LLMResponse> get(String prompt, String model, String providerId) {
    int key = generateKey(prompt, model, providerId);
    CacheEntry entry = cache.get(key);

    if (entry != null) {
        if (System.currentTimeMillis() - entry.timestamp < TTL_MS) {
            hitCount.incrementAndGet();
            // Atomic access time update
            accessTimestamps.put(key, counter.getAndIncrement());
            return Optional.of(entry.response);
        } else {
            // Atomic remove-if-present
            cache.remove(key, entry);
            accessTimestamps.remove(key);
        }
    }

    missCount.incrementAndGet();
    return Optional.empty();
}

public void put(String prompt, String model, String providerId, LLMResponse response) {
    int key = generateKey(prompt, model, providerId);

    // Atomic compute-if-absent
    cache.compute(key, (k, existing) -> {
        if (existing != null && cache.size() >= MAX_CACHE_SIZE) {
            // Need to evict - find LRU
            evictSingleOldest();
        }
        return new CacheEntry(response.withCacheFlag(true));
    });

    accessTimestamps.put(key, counter.getAndIncrement());
}

private void evictSingleOldest() {
    // Find and remove single oldest entry atomically
    accessTimestamps.entrySet().stream()
        .min(Map.Entry.comparingByValue())
        .ifPresent(entry -> {
            cache.remove(entry.getKey());
            accessTimestamps.remove(entry.getKey());
        });
}
```

---

#### 4. RACE: OrchestratorService worker assignment check-then-act

**Location:** `OrchestratorService.java:364-384`
**Severity:** MEDIUM

**Problem:**
```java
// Line 365-368
List<ForemanEntity> availableWorkers = availableSteves.stream()
    .filter(s -> !s.getEntityName().equals(foremanId))
    .filter(s -> !workerAssignments.containsKey(s.getEntityName()))
    .collect(Collectors.toList());
```

The stream filters are **not atomic** with the subsequent assignment. Between filtering and assigning, a worker could be assigned by another thread, leading to:
- Double assignment of tasks
- Workers receiving multiple tasks simultaneously

**Fix:**
```java
private void distributeTasks(PlanExecution plan, Collection<ForemanEntity> availableSteves) {
    List<Task> tasks = plan.getRemainingTasks();

    int workerIndex = 0;
    for (Task task : tasks) {
        // Atomic claim of worker
        String workerId = claimNextAvailableWorker(availableSteves);
        if (workerId == null) {
            assignTaskToAgent(plan, task, foremanId);
        } else {
            assignTaskToAgent(plan, task, workerId);
            workerIndex++;
        }
    }
}

private String claimNextAvailableWorker(Collection<ForemanEntity> availableSteves) {
    for (ForemanEntity steve : availableSteves) {
        String entityId = steve.getEntityName();
        if (entityId.equals(foremanId)) continue;

        // Atomic put-if-absent
        TaskAssignment dummy = new TaskAssignment(null, null, null);
        TaskAssignment existing = workerAssignments.putIfAbsent(entityId, dummy);

        if (existing == null) {
            // Successfully claimed this worker
            return entityId;
        }
    }
    return null; // No available workers
}
```

---

#### 5. RACE: CompanionMemory emotional memory list modification

**Location:** `CompanionMemory.java:270-286`
**Severity:** MEDIUM

**Problem:**
```java
// Line 274: Adding to non-thread-safe list
emotionalMemories.add(memory);

// Line 277-279: Sorting without synchronization
emotionalMemories.sort((a, b) -> Integer.compare(
    Math.abs(b.emotionalWeight), Math.abs(a.emotionalWeight)
));

// Line 282-284: Remove without synchronization
if (emotionalMemories.size() > 50) {
    emotionalMemories.remove(emotionalMemories.size() - 1);
}
```

`emotionalMemories` is an `ArrayList` accessed from multiple threads (tick thread + LLM callbacks). This can cause:
- `ConcurrentModificationException`
- Lost updates
- Corruption during sort

**Fix:**
```java
private final List<EmotionalMemory> emotionalMemories =
    Collections.synchronizedList(new ArrayList<>());

// OR better:
private final CopyOnWriteArrayList<EmotionalMemory> emotionalMemories =
    new CopyOnWriteArrayList<>();
```

---

#### 6. RACE: ForemanEntity role change without proper synchronization

**Location:** `ForemanEntity.java:810-823`
**Severity:** LOW

**Problem:**
```java
public void setRole(AgentRole newRole) {
    AgentRole oldRole = this.role;
    this.role = newRole;  // Not volatile or atomic

    // Re-register with new role
    if (orchestrator != null && registeredWithOrchestrator.get()) {
        orchestrator.unregisterAgent(entityName);
        orchestrator.registerAgent(this, newRole);
        registeredWithOrchestrator.set(true);
    }
```

The `role` field is not volatile, and the check-then-act sequence for re-registration is not atomic. Another thread could see an inconsistent role state.

**Fix:**
```java
private volatile AgentRole role = AgentRole.SOLO;
private final Object roleLock = new Object();

public void setRole(AgentRole newRole) {
    synchronized (roleLock) {
        AgentRole oldRole = this.role;
        this.role = newRole;

        if (orchestrator != null && registeredWithOrchestrator.get()) {
            orchestrator.unregisterAgent(entityName);
            orchestrator.registerAgent(this, newRole);
        }
    }

    sendChatMessage(String.format("Role changed: %s -> %s",
        oldRole.getDisplayName(), newRole.getDisplayName()));
}
```

---

### DEADLOCK: Deadlock Risks

#### 1. DEADLOCK: OrchestratorService nested map locks

**Location:** `OrchestratorService.java:489-531`
**Severity:** MEDIUM

**Problem:**
```java
private void handleTaskComplete(String workerId, AgentMessage message) {
    // Line 489: Acquires workerAssignments lock
    TaskAssignment assignment = workerAssignments.remove(workerId);

    if (assignment != null) {
        // Line 495-498: Acquires activePlans lock while holding workerAssignments
        PlanExecution plan = activePlans.get(assignment.getParentPlanId());
        if (plan != null) {
            plan.markTaskComplete(assignment.getAssignmentId());
            checkPlanCompletion(plan);
        }
    }
}

private void handleTaskFailed(String workerId, AgentMessage message) {
    // Similar pattern - acquires workerAssignments, then activePlans
    TaskAssignment assignment = workerAssignments.remove(workerId);
    if (assignment != null) {
        PlanExecution plan = activePlans.get(assignment.getParentPlanId());
```

**Potential deadlock scenario:**
```
Thread A: handleTaskComplete()
  - Locks workerAssignments
  - Tries to lock activePlans

Thread B: distributeTasks()
  - Locks activePlans (via plan.getRemainingTasks())
  - Tries to lock workerAssignments (via assignTaskToAgent)
```

**Fix: Establish lock ordering:**
```java
// Always lock activePlans BEFORE workerAssignments
private final Object planLock = new Object();
private final Object assignmentLock = new Object();

private void handleTaskComplete(String workerId, AgentMessage message) {
    TaskAssignment assignment;
    synchronized (assignmentLock) {
        assignment = workerAssignments.remove(workerId);
    }

    if (assignment != null) {
        synchronized (planLock) {  // Always lock plans first
            PlanExecution plan = activePlans.get(assignment.getParentPlanId());
            if (plan != null) {
                plan.markTaskComplete(assignment.getAssignmentId());
                checkPlanCompletion(plan);
            }
        }
    }
}
```

---

#### 2. DEADLOCK: CompanionMemory vector store operations

**Location:** `CompanionMemory.java:213-231, 410-429`
**Severity:** LOW

**Problem:**
```java
// recordExperience holds lock on episodicMemories (implicit via addFirst)
// then acquires memoryToVectorId lock
public void recordExperience(...) {
    episodicMemories.addFirst(memory);
    addMemoryToVectorStore(memory);  // Locks memoryToVectorId
    while (episodicMemories.size() > MAX_EPISODIC_MEMORIES) {
        EpisodicMemory removed = episodicMemories.removeLast();
        Integer vectorId = memoryToVectorId.remove(removed);  // Locks memoryToVectorId again
```

While ConcurrentHashMap handles individual operations safely, compound operations across multiple maps can lead to inconsistent state.

**Fix:** Use striped locks or reduce lock scope:
```java
private final Striped<Lock> memoryLocks = Striped.lock(16);

public void recordExperience(...) {
    EpisodicMemory memory = new EpisodicMemory(...);
    Lock lock = memoryLocks.get(memory);

    lock.lock();
    try {
        episodicMemories.addFirst(memory);
        addMemoryToVectorStore(memory);
        trimMemoriesIfNeeded();
    } finally {
        lock.unlock();
    }
}
```

---

#### 3. DEADLOCK: AgentCommunicationBus handler invocation

**Location:** `AgentCommunicationBus.java:228-243`
**Severity:** LOW

**Problem:**
```java
private void notifyHandlers(String agentId) {
    List<Consumer<AgentMessage>> handlers = subscribers.get(agentId);
    PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);

    if (handlers != null && queue != null) {
        AgentMessage message = queue.poll();
        if (message != null) {
            for (Consumer<AgentMessage> handler : handlers) {
                try {
                    handler.accept(message);  // User code could block or re-enter bus
                } catch (Exception e) {
                    LOGGER.error("Error in message handler...", e);
                }
            }
        }
    }
}
```

If a handler attempts to publish another message, it could deadlock if queues are full.

**Fix:**
```java
private final ExecutorService handlerExecutor =
    Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "message-handler");
        t.setDaemon(true);
        return t;
    });

private void notifyHandlers(String agentId) {
    List<Consumer<AgentMessage>> handlers = subscribers.get(agentId);
    PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);

    if (handlers != null && queue != null) {
        AgentMessage message = queue.poll();
        if (message != null) {
            // Execute handlers asynchronously to prevent deadlock
            for (Consumer<AgentMessage> handler : handlers) {
                handlerExecutor.submit(() -> {
                    try {
                        handler.accept(message);
                    } catch (Exception e) {
                        LOGGER.error("Error in message handler", e);
                    }
                });
            }
        }
    }
}
```

---

#### 4. DEADLOCK: InterceptorChain synchronous iteration

**Location:** `InterceptorChain.java:112-124, 163-173`
**Severity:** LOW

**Problem:**
```java
public boolean executeBeforeAction(BaseAction action, ActionContext context) {
    for (ActionInterceptor interceptor : interceptors) {
        try {
            if (!interceptor.beforeAction(action, context)) {
                return false;
            }
        } catch (Exception e) {
            logInterceptorError(interceptor, "beforeAction", e);
        }
    }
    return true;
}
```

If an interceptor's `beforeAction()` method attempts to modify the interceptor chain (e.g., add/remove interceptors), it will cause a `ConcurrentModificationException` on CopyOnWriteArrayList.

**Fix:** Document clearly or use snapshot:
```java
public boolean executeBeforeAction(BaseAction action, ActionContext context) {
    // Take snapshot to prevent CME if interceptors are modified during iteration
    List<ActionInterceptor> snapshot = new ArrayList<>(interceptors);

    for (ActionInterceptor interceptor : snapshot) {
        try {
            if (!interceptor.beforeAction(action, context)) {
                return false;
            }
        } catch (Exception e) {
            logInterceptorError(interceptor, "beforeAction", e);
        }
    }
    return true;
}
```

---

### ESCAPE: Object Escape Issues

#### 1. ESCAPE: AsyncOpenAIClient static executor leakage

**Location:** `AsyncOpenAIClient.java:68-89`
**Severity:** HIGH

**Problem:**
```java
// Line 68-73: Static ScheduledExecutorService created at class load time
private static final ScheduledExecutorService RETRY_SCHEDULER =
    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "openai-retry-scheduler");
        t.setDaemon(true);
        return t;
    });
```

**Issues:**
1. **Never shut down properly:** The `shutdown()` method is static but never called automatically
2. **Thread leaks:** Each AsyncOpenAIClient instance shares the same scheduler, but shutdown affects all instances
3. **Classloader leaks:** In modded environments, this can prevent proper class unloading

**Fix:**
```java
private static final ScheduledExecutorService RETRY_SCHEDULER =
    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "openai-retry-scheduler");
        t.setDaemon(true);
        return t;
    });

// Add shutdown hook for automatic cleanup
static {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        shutdown();
    }));
}

public static void shutdown() {
    if (!RETRY_SCHEDULER.isShutdown()) {
        RETRY_SCHEDULER.shutdown();
        try {
            if (!RETRY_SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                RETRY_SCHEDLER.shutdownNow();
            }
        } catch (InterruptedException e) {
            RETRY_SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

---

#### 2. ESCAPE: LLMExecutorService singleton not properly initialized

**Location:** `LLMExecutorService.java:46-80`
**Severity:** MEDIUM

**Problem:**
```java
// Line 46: Eager initialization at class load time
private static final LLMExecutorService INSTANCE = new LLMExecutorService();

// Line 61-80: Constructor creates thread pools
private LLMExecutorService() {
    LOGGER.info("Initializing LLM executor service...");

    this.openaiExecutor = Executors.newFixedThreadPool(
        THREADS_PER_PROVIDER,
        new NamedThreadFactory("llm-openai")
    );
    // ... more executors
}
```

If an exception occurs during construction, the singleton will be in a partially initialized state but still non-null.

**Fix:** Use lazy initialization with proper error handling:
```java
private static volatile LLMExecutorService INSTANCE;
private static final Object INIT_LOCK = new Object();

public static LLMExecutorService getInstance() {
    LLMExecutorService instance = INSTANCE;
    if (instance == null) {
        synchronized (INIT_LOCK) {
            instance = INSTANCE;
            if (instance == null) {
                try {
                    INSTANCE = instance = new LLMExecutorService();
                } catch (Exception e) {
                    LOGGER.error("Failed to initialize LLMExecutorService", e);
                    throw new RuntimeException("LLMExecutorService initialization failed", e);
                }
            }
        }
    }
    return instance;
}
```

---

#### 3. ESCAPE: ForemanEntity publishes 'this' before fully constructed

**Location:** `ForemanEntity.java:226-243`
**Severity:** MEDIUM

**Problem:**
```java
public ForemanEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
    super(entityType, level);  // 'this' escapes to parent class

    this.entityName = "Foreman";
    this.memory = new ForemanMemory(this);  // 'this' passed to memory
    this.companionMemory = new CompanionMemory();
    this.actionExecutor = new ActionExecutor(this);  // 'this' passed to executor
    this.dialogueManager = new ProactiveDialogueManager(this);
    this.tacticalService = TacticalDecisionService.getInstance();

    // ... more initialization

    if (!level.isClientSide) {
        this.orchestrator = MineWrightMod.getOrchestratorService();
        // 'this' could be registered with orchestrator before construction completes
    }
}
```

If another thread accesses the entity via the orchestrator or action executor before construction completes, it will see an incompletely initialized object.

**Fix:** Don't pass `this` to other components during construction:
```java
public ForemanEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
    super(entityType, level);

    this.entityName = "Foreman";
    // Initialize thread-safe components without passing 'this'
    this.memory = new ForemanMemory(null);
    this.companionMemory = new CompanionMemory();
    this.tacticalService = TacticalDecisionService.getInstance();

    // Set up orchestrator reference only
    this.orchestrator = !level.isClientSide ?
        MineWrightMod.getOrchestratorService() : null;

    // Lazy initialize components that need 'this'
    this.actionExecutor = null;
    this.dialogueManager = null;
}

// Call during first tick or explicitly
public void postConstruct() {
    if (actionExecutor == null) {
        this.actionExecutor = new ActionExecutor(this);
        this.dialogueManager = new ProactiveDialogueManager(this);
        memory.setForeman(this);
    }
}
```

---

### BLOCKING: Blocking Operations

#### 1. BLOCKING: ActionExecutor tick() blocks for 60 seconds

**Location:** `ActionExecutor.java:395`
**Severity:** CRITICAL

**Problem:**
```java
// Line 395: BLOCKING CALL ON MINECRAFT SERVER THREAD
ResponseParser.ParsedResponse response = planningFuture.get(60, TimeUnit.SECONDS);
```

This is called every tick on the Minecraft server thread. If the LLM takes 30 seconds to respond, the server freezes for 30 seconds.

**Fix:** Use `getNow()` or `poll()` instead:
```java
if (isPlanning && planningFuture != null && planningFuture.isDone()) {
    try {
        // Non-blocking immediate retrieval
        ResponseParser.ParsedResponse response = planningFuture.getNow(null);

        if (response != null) {
            // Process response
            currentGoal = response.getPlan();
            foreman.getMemory().setCurrentGoal(currentGoal);
            taskQueue.clear();
            taskQueue.addAll(response.getTasks());
            // ...
        } else {
            // Future completed exceptionally
            LOGGER.warn("Planning future completed exceptionally");
            stateMachine.forceTransition(AgentState.IDLE, "planning failed");
        }
    } finally {
        isPlanning = false;
        planningFuture = null;
        pendingCommand = null;
    }
}
```

---

#### 2. BLOCKING: AgentCommunicationBus poll() with busy-wait

**Location:** `AgentCommunicationBus.java:272-288`
**Severity:** MEDIUM

**Problem:**
```java
public AgentMessage poll(String agentId, long timeout, TimeUnit unit) throws InterruptedException {
    PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
    if (queue != null) {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            AgentMessage message = queue.poll();
            if (message != null) {
                stats.recordReceived();
                return message;
            }
            Thread.sleep(10);  // BUSY WAIT
        }
    }
    return null;
}
```

Busy-wait loop consumes CPU unnecessarily.

**Fix:**
```java
public AgentMessage poll(String agentId, long timeout, TimeUnit unit) throws InterruptedException {
    PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
    if (queue != null) {
        return queue.poll(timeout, unit);  // Use built-in timed poll
    }
    return null;
}
```

---

#### 3. BLOCKING: ConversationManager doesn't handle async errors properly

**Location:** `ConversationManager.java:88-128`
**Severity:** LOW

**Problem:**
```java
return llmClient.sendAsync(userPrompt, params)
    .thenApply(response -> {
        String responseText = response.getContent().trim();
        memory.incrementInteractionCount();
        memory.adjustRapport(1);
        trackPhraseUsage(responseText);
        return responseText;
    })
    .exceptionally(error -> {
        LOGGER.error("Failed to generate conversational response", error);
        return "I'm having trouble thinking of a response right now.";
    });
```

If `memory.adjustRapport()` or `trackPhraseUsage()` throws an exception, it will propagate to the caller instead of being caught by `exceptionally`.

**Fix:**
```java
return llmClient.sendAsync(userPrompt, params)
    .thenApply(response -> {
        String responseText = response.getContent().trim();
        try {
            memory.incrementInteractionCount();
            memory.adjustRapport(1);
            trackPhraseUsage(responseText);
        } catch (Exception e) {
            LOGGER.error("Error updating memory state", e);
            // Continue anyway - memory update is not critical
        }
        return responseText;
    })
    .exceptionally(error -> {
        LOGGER.error("Failed to generate conversational response", error);
        return "I'm having trouble thinking of a response right now.";
    });
```

---

#### 4. BLOCKING: ActionExecutor queueTask doesn't handle backpressure

**Location:** `ActionExecutor.java:650-664`
**Severity:** LOW

**Problem:**
```java
public void queueTask(Task task) {
    if (task == null) {
        MineWrightMod.LOGGER.warn("Attempted to queue null task");
        return;
    }

    // offer() can fail if queue is full (though LinkedBlockingQueue is unbounded by default)
    if (taskQueue.offer(task)) {
        MineWrightMod.LOGGER.info("Task queued: {}", task.getAction());
    } else {
        MineWrightMod.LOGGER.warn("Failed to queue task (queue full): {}", task.getAction());
    }
}
```

While `LinkedBlockingQueue` is unbounded by default, this could lead to memory exhaustion if tasks are queued faster than they're executed.

**Fix:**
```java
private final BlockingQueue<Task> taskQueue =
    new LinkedBlockingQueue<>(100); // Bounded queue with 100 capacity

public CompletableFuture<Void> queueTask(Task task) {
    return CompletableFuture.runAsync(() -> {
        try {
            // Blocking put with timeout to apply backpressure
            if (!taskQueue.offer(task, 5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Task queue full - task rejected");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while queueing task", e);
        }
    });
}
```

---

#### 5. BLOCKING: OrchestratorService retry task could block indefinitely

**Location:** `OrchestratorService.java:537-560`
**Severity:** LOW

**Problem:**
```java
private void retryTask(TaskAssignment assignment) {
    Optional<String> newWorker = workerRegistry.keySet().stream()
        .filter(id -> !id.equals(assignment.getAssignedWorkerId()))
        .filter(id -> !workerAssignments.containsKey(id))
        .findFirst();

    if (newWorker.isPresent()) {
        String newWorkerId = newWorker.get();
        assignment.reassign(newWorkerId, "Retry after failure");
        workerAssignments.put(newWorkerId, assignment);

        AgentMessage retryMessage = AgentMessage.taskAssignment(...);
        communicationBus.publish(retryMessage);
    }
}
```

If all workers are busy, this silently fails to retry. The task remains in a failed state with no indication to the user.

**Fix:**
```java
private void retryTask(TaskAssignment assignment) {
    Optional<String> newWorker = workerRegistry.keySet().stream()
        .filter(id -> !id.equals(assignment.getAssignedWorkerId()))
        .filter(id -> !workerAssignments.containsKey(id))
        .findFirst();

    if (newWorker.isPresent()) {
        String newWorkerId = newWorker.get();
        assignment.reassign(newWorkerId, "Retry after failure");

        // Atomic put-if-absent to avoid race with new assignments
        TaskAssignment existing = workerAssignments.putIfAbsent(newWorkerId, assignment);
        if (existing != null) {
            // Worker got assigned another task - schedule retry
            scheduleRetryLater(assignment);
            return;
        }

        AgentMessage retryMessage = AgentMessage.taskAssignment(...);
        communicationBus.publish(retryMessage);
    } else {
        // No available workers - notify and queue for later
        LOGGER.warn("No available workers for task retry - queuing for later");
        scheduleRetryLater(assignment);
        notifyHumanPlayer("No workers available - task will retry when one is free");
    }
}
```

---

#### 6. BLOCKING: ForemanMemory NBT save/load on main thread

**Location:** `ForemanEntity.java:417-447`
**Severity:** MEDIUM

**Problem:**
```java
@Override
public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putString("CrewName", this.entityName);

    CompoundTag memoryTag = new CompoundTag();
    this.memory.saveToNBT(memoryTag);  // Can be slow for large memories
    tag.put("Memory", memoryTag);

    CompoundTag companionMemoryTag = new CompoundTag();
    this.companionMemory.saveToNBT(companionMemoryTag);  // Can be very slow
    tag.put("CompanionMemory", companionMemoryTag);
}
```

NBT serialization happens on the main game thread during world save. Large memory objects can cause significant lag spikes.

**Fix:** Move NBT serialization to async thread:
```java
@Override
public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putString("CrewName", this.entityName);

    // Use pre-computed cache from last save
    if (cachedMemoryTag != null) {
        tag.put("Memory", cachedMemoryTag);
    }
    if (cachedCompanionMemoryTag != null) {
        tag.put("CompanionMemory", cachedCompanionMemoryTag);
    }
}

// Update cache asynchronously every minute
@SubscribeEvent
public void onTick(TickEvent.ServerTickEvent event) {
    if (event.phase == TickEvent.Phase.END) {
        if (level.getGameTime() % 1200 == 0) {  // Every minute
            updateMemoryCacheAsync();
        }
    }
}
```

---

#### 7. BLOCKING: AgentStateMachine forceTransition doesn't validate

**Location:** `AgentStateMachine.java:220-231`
**Severity:** LOW

**Problem:**
```java
public void forceTransition(AgentState targetState, String reason) {
    if (targetState == null) return;

    AgentState fromState = currentState.getAndSet(targetState);
    LOGGER.warn("[{}] FORCED state transition: {} -> {} (reason: {})",
        agentId, fromState, targetState, reason);

    if (eventBus != null) {
        eventBus.publish(new StateTransitionEvent(agentId, fromState, targetState,
            "FORCED: " + reason));
    }
}
```

Force transitions bypass validation but can still fail if event bus publishing blocks. Also, this could be abused to create invalid state sequences.

**Fix:**
```java
public void forceTransition(AgentState targetState, String reason) {
    if (targetState == null) return;

    AgentState fromState = currentState.getAndSet(targetState);
    LOGGER.warn("[{}] FORCED state transition: {} → {} (reason: {})",
        agentId, fromState, targetState, reason);

    // Publish event asynchronously to avoid blocking
    if (eventBus != null) {
        StateTransitionEvent event = new StateTransitionEvent(
            agentId, fromState, targetState, "FORCED: " + reason
        );
        // Publish without waiting
        try {
            eventBus.publish(event);
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to publish state transition event", agentId, e);
        }
    }
}
```

---

### SAFE: Correctly Synchronized Code

#### 1. SAFE: AgentStateMachine uses AtomicReference correctly

**Location:** `AgentStateMachine.java:99-209`
**Assessment:** EXCELLENT

**Why it's safe:**
```java
private final AtomicReference<AgentState> currentState;

public boolean transitionTo(AgentState targetState, String reason) {
    AgentState fromState = currentState.get();

    if (!canTransitionTo(targetState)) {
        return false;
    }

    // Atomic compare-and-set for thread safety
    if (currentState.compareAndSet(fromState, targetState)) {
        LOGGER.info("[{}] State transition: {} → {}", agentId, fromState, targetState);

        if (eventBus != null) {
            eventBus.publish(new StateTransitionEvent(agentId, fromState, targetState, reason));
        }

        return true;
    } else {
        // State changed between get and compareAndSet (race condition handled)
        LOGGER.warn("[{}] State transition failed: concurrent modification", agentId);
        return false;
    }
}
```

**Commendable practices:**
- Uses `AtomicReference` for state
- Proper `compareAndSet()` pattern
- Handles concurrent modification gracefully
- Validation before mutation

---

#### 2. SAFE: LLMExecutorService shutdown is synchronized

**Location:** `LLMExecutorService.java:125-139`
**Assessment:** GOOD

**Why it's safe:**
```java
public synchronized void shutdown() {
    if (isShutdown) {
        LOGGER.debug("LLMExecutorService already shut down, ignoring duplicate shutdown call");
        return;
    }

    LOGGER.info("Shutting down LLM executor service...");
    isShutdown = true;

    shutdownExecutor("openai", openaiExecutor);
    shutdownExecutor("groq", groqExecutor);
    shutdownExecutor("gemini", geminiExecutor);

    LOGGER.info("LLM executor service shut down successfully");
}
```

**Commendable practices:**
- Synchronized shutdown prevents concurrent shutdowns
- Idempotent (can be called multiple times)
- Properly waits for termination with timeout
- Falls back to shutdownNow() if graceful shutdown fails

---

#### 3. SAFE: CompanionMemory uses AtomicInteger for counters

**Location:** `CompanionMemory.java:99-109, 569-582`
**Assessment:** GOOD

**Why it's safe:**
```java
private final AtomicInteger rapportLevel;
private final AtomicInteger trustLevel;
private final AtomicInteger interactionCount;

public void adjustRapport(int delta) {
    int newValue = Math.max(0, Math.min(100, rapportLevel.get() + delta));
    rapportLevel.set(newValue);
}

public void adjustTrust(int delta) {
    int newValue = Math.max(0, Math.min(100, trustLevel.get() + delta));
    trustLevel.set(newValue);
}
```

**Commendable practices:**
- Uses `AtomicInteger` for shared counters
- Proper bounds checking (0-100)
- Thread-safe reads and writes
- Increment/decrement via `incrementAndGet()`

---

## Recommendations

### Immediate Actions (Critical)

1. **Fix blocking ActionExecutor.tick()**
   - Replace `planningFuture.get(60, SECONDS)` with `getNow()`
   - Prevents server freezes during LLM calls

2. **Fix LLMCache race conditions**
   - Make LRU operations atomic
   - Prevents cache corruption

3. **Fix AsyncOpenAIClient executor leakage**
   - Add proper shutdown hook
   - Prevents thread leaks in modded environment

### Short-term (High Priority)

4. **Establish lock ordering in OrchestratorService**
   - Always lock `activePlans` before `workerAssignments`
   - Prevents deadlocks

5. **Make ActionExecutor state transitions atomic**
   - Use `AtomicInteger` for planning state
   - Prevents duplicate planning requests

6. **Fix CompanionMemory list thread safety**
   - Use `CopyOnWriteArrayList` for `emotionalMemories`
   - Prevents CME

### Long-term (Medium Priority)

7. **Add comprehensive concurrency testing**
   - Use `JCStress` for concurrency testing
   - Add stress tests for cache operations

8. **Consider using immutable data structures**
   - Reduces synchronization needs
   - Makes code easier to reason about

9. **Add thread-safety documentation**
   - Document which methods are thread-safe
   - Use `@ThreadSafe`, `@NotThreadSafe` annotations

---

## Testing Recommendations

### Concurrency Tests to Add

```java
// Test ActionExecutor concurrent planning
@Test
public void testConcurrentPlanningRequests() throws Exception {
    ActionExecutor executor = new ActionExecutor(foreman);
    CountDownLatch latch = new CountDownLatch(10);
    AtomicInteger successCount = new AtomicInteger(0);

    for (int i = 0; i < 10; i++) {
        new Thread(() -> {
            try {
                executor.processNaturalLanguageCommand("test command");
                successCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        }).start();
    }

    latch.await(5, TimeUnit.SECONDS);
    assertEquals("Only one planning request should succeed", 1, successCount.get());
}

// Test LLMCache concurrent access
@Test
public void testConcurrentCacheAccess() throws Exception {
    LLMCache cache = new LLMCache();
    LLMResponse response = LLMResponse.builder()
        .content("test")
        .model("gpt-4")
        .providerId("openai")
        .build();

    int threads = 10;
    int operationsPerThread = 1000;
    CountDownLatch latch = new CountDownLatch(threads);
    AtomicInteger errors = new AtomicInteger(0);

    for (int t = 0; t < threads; t++) {
        final int threadId = t;
        new Thread(() -> {
            try {
                for (int i = 0; i < operationsPerThread; i++) {
                    String prompt = "test" + threadId + "-" + i;
                    cache.put(prompt, "gpt-4", "openai", response);
                    cache.get(prompt, "gpt-4", "openai");
                }
            } catch (Exception e) {
                errors.incrementAndGet();
            } finally {
                latch.countDown();
            }
        }).start();
    }

    latch.await(30, TimeUnit.SECONDS);
    assertEquals("No concurrent access errors", 0, errors.get());
}
```

---

## Conclusion

The MineWright mod demonstrates **good awareness** of thread safety with appropriate use of:
- `ConcurrentHashMap` for shared maps
- `volatile` for visibility
- `AtomicBoolean` for flags
- `CopyOnWriteArrayList` for interceptor lists

However, there are **critical issues** that need addressing:
1. **Blocking operations on the server thread** (60-second waits)
2. **Race conditions in cache operations** (potential corruption)
3. **Improper lock ordering** (potential deadlocks)
4. **Resource leaks** (static executors)

With the fixes recommended above, the codebase will be significantly more robust and reliable in multi-threaded scenarios.

---

**Report Generated:** 2026-02-27
**Reviewer:** Thread Safety and Concurrency Review Specialist
**Files Reviewed:** 12 core files, ~4000 lines of code
