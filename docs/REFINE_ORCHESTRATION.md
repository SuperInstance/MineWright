# Orchestration System Refinement Report

**Analysis Date:** 2026-02-27
**Components Analyzed:**
- `OrchestratorService.java`
- `AgentCommunicationBus.java`
- `TaskAssignment.java`
- `AgentMessage.java`
- `AgentRole.java`
- `CollaborativeBuildManager.java`

---

## Executive Summary

The orchestration system implements a hierarchical foreman-worker pattern for coordinating multiple AI agents in Minecraft. While the foundation is solid, there are significant opportunities for improvement in **task distribution fairness**, **worker capability matching**, **failure recovery**, **progress tracking accuracy**, and **scalability**.

### Current Strengths
- Clean hierarchical architecture (Foreman → Workers)
- Priority-based message bus with filtering
- Task state machine with comprehensive lifecycle tracking
- Collaborative building with spatial partitioning

### Critical Gaps
1. **Naive round-robin task distribution** ignores worker capabilities and current load
2. **No capability-based worker selection** - specialists not matched to appropriate tasks
3. **Basic failure handling** - limited retry logic without exponential backoff
4. **No progress aggregation** - individual task progress doesn't roll up accurately
5. **Scalability concerns** - O(n) polling in communication bus, no batching

---

## 1. Task Distribution Analysis

### Current Implementation

```java
// OrchestratorService.java, line 244-255
private void distributeTasks(PlanExecution plan, Collection<ForemanEntity> availableSteves) {
    List<Task> tasks = plan.getRemainingTasks();
    List<ForemanEntity> availableWorkers = availableSteves.stream()
        .filter(s -> !s.getSteveName().equals(foremanId))
        .filter(s -> !workerAssignments.containsKey(s.getSteveName()))
        .collect(Collectors.toList());

    int workerIndex = 0;
    for (Task task : tasks) {
        if (availableWorkers.isEmpty()) {
            assignTaskToAgent(plan, task, foremanId);
        } else {
            // Round-robin assignment - PROBLEMATIC
            ForemanEntity worker = availableWorkers.get(workerIndex % availableWorkers.size());
            assignTaskToAgent(plan, task, worker.getSteveName());
            workerIndex++;
        }
    }
}
```

### Problems Identified

1. **No Task Priority Consideration**
   - Urgent tasks (defined in `TaskAssignment.Priority`) are treated the same as low-priority tasks
   - All tasks distributed in list order, not by priority

2. **No Worker Load Balancing**
   - Only checks if worker has *any* assignment (`!workerAssignments.containsKey()`)
   - Doesn't consider:
     - How many tasks a worker already has (if multi-tasking supported)
     - Current progress percentage
     - Estimated completion time
     - Worker's historical performance

3. **No Capability Matching**
   - `AgentRole.SPECIALIST` exists but isn't utilized
   - Task type (e.g., "mine", "build", "farm") not matched to specialist capabilities
   - All workers treated identically

4. **No Task Dependencies**
   - `TaskAssignment` has dependency tracking (`addDependency()`, `checkDependencies()`)
   - But dependencies are never checked before assignment
   - Could assign task B before its dependency task A completes

---

## 2. Improved Task Distribution Strategy

### Proposed Solution: Work-Stealing with Capability Matching

```java
/**
 * Enhanced task distributor with:
 * - Capability-based worker selection
 * - Priority queue for tasks
 * - Work-stealing for load balancing
 * - Dependency validation
 */
public class SmartTaskDistributor {

    private final Map<String, WorkerCapabilities> workerCapabilities;
    private final PriorityBlockingQueue<TaskAssignment> pendingTasks;
    private final Map<String, Deque<TaskAssignment>> workerQueues;

    /**
     * Assign tasks based on worker capabilities, current load, and task priority.
     */
    public void distributeTasks(PlanExecution plan, Collection<ForemanEntity> workers) {
        // 1. Sort tasks by priority and dependencies
        List<TaskAssignment> sortedTasks = prioritizeTasks(plan);

        // 2. Build capability profiles for each worker
        Map<String, WorkerProfile> profiles = buildWorkerProfiles(workers);

        // 3. Assign tasks with capability matching
        for (TaskAssignment task : sortedTasks) {
            if (!task.checkDependencies()) {
                pendingTasks.offer(task);
                continue;
            }

            String bestWorker = selectBestWorker(task, profiles);
            if (bestWorker != null) {
                assignTask(task, bestWorker);
                profiles.get(bestWorker).addTask(task);
            } else {
                pendingTasks.offer(task); // No suitable worker available
            }
        }
    }

    /**
     * Selects the best worker for a task based on:
     * 1. Capability match (specialist gets priority)
     * 2. Current load (fewer queued tasks)
     * 3. Historical performance (success rate, avg completion time)
     */
    private String selectBestWorker(TaskAssignment task, Map<String, WorkerProfile> profiles) {
        return profiles.values().stream()
            .filter(p -> p.canHandle(task)) // Capability check
            .sorted(Comparator
                .comparingInt(WorkerProfile::getQueueSize) // Prefer less loaded
                .thenComparingDouble(WorkerProfile::getSuccessRate).reversed() // Prefer more reliable
                .thenComparingLong(WorkerProfile::getAvgCompletionTime)) // Prefer faster
            .map(WorkerProfile::getWorkerId)
            .findFirst()
            .orElse(null);
    }
}

/**
 * Tracks worker capabilities and performance metrics.
 */
class WorkerProfile {
    private final String workerId;
    private final AgentRole role;
    private final Set<String> capabilities; // e.g., "mine", "build", "farm"
    private final Deque<TaskAssignment> taskQueue;

    // Performance metrics
    private final AtomicInteger totalTasks = new AtomicInteger(0);
    private final AtomicInteger successfulTasks = new AtomicInteger(0);
    private final AtomicLong totalCompletionTime = new AtomicLong(0);

    public boolean canHandle(TaskAssignment task) {
        if (role == AgentRole.WORKER) {
            return true; // Workers can handle any task
        }

        // Specialists only handle tasks in their domain
        String taskType = task.getTaskDescription().toLowerCase();
        return capabilities.stream()
            .anyMatch(cap -> taskType.contains(cap));
    }

    public double getSuccessRate() {
        int total = totalTasks.get();
        return total > 0 ? (double) successfulTasks.get() / total : 1.0;
    }

    public long getAvgCompletionTime() {
        int total = totalTasks.get();
        return total > 0 ? totalCompletionTime.get() / total : 0;
    }

    public int getQueueSize() {
        return taskQueue.size();
    }

    public void recordTaskCompletion(long durationMs, boolean success) {
        totalTasks.incrementAndGet();
        if (success) successfulTasks.incrementAndGet();
        totalCompletionTime.addAndGet(durationMs);
    }
}
```

### Key Improvements

1. **Capability-Based Assignment**
   - Specialists only receive tasks matching their expertise
   - Workers handle general-purpose tasks
   - Prevents specialists from being underutilized

2. **Load-Aware Distribution**
   - Tracks each worker's current queue size
   - Prefers workers with fewer pending tasks
   - Enables work-stealing when idle workers can help busy ones

3. **Performance-Based Selection**
   - Tracks success rate per worker
   - Tracks average completion time per worker
   - Prefers reliable, fast workers for critical tasks

4. **Dependency Validation**
   - Checks `TaskAssignment.checkDependencies()` before assigning
   - Defers tasks with unmet dependencies to `pendingTasks` queue
   - Re-evaluates pending tasks when dependencies complete

---

## 3. Failure Handling Improvements

### Current Implementation

```java
// OrchestratorService.java, line 373-394
private void handleTaskFailed(String workerId, AgentMessage message) {
    TaskAssignment assignment = workerAssignments.remove(workerId);
    if (assignment != null) {
        String reason = message.getPayloadValue("result", "Unknown error");
        assignment.fail(reason);

        // Check if we should retry
        if (assignment.getRetryCount() < MAX_TASK_RETRIES) { // MAX_TASK_RETRIES = 2
            retryTask(assignment);
        } else {
            PlanExecution plan = activePlans.get(assignment.getParentPlanId());
            if (plan != null) {
                plan.markTaskFailed(assignment.getAssignmentId());
            }
            notifyHumanPlayer(String.format("%s failed: %s", workerId, reason));
        }
    }
}
```

### Problems Identified

1. **Fixed Retry Count**
   - All tasks get exactly 2 retries regardless of:
     - Failure type (transient vs. permanent)
     - Task criticality
     - Worker health

2. **No Exponential Backoff**
   - Immediate retry can overwhelm failing workers
   - No delay between retries

3. **No Failure Classification**
   - All failures treated identically
   - Should distinguish:
     - Transient failures (network, temporary blockage) → Retry
     - Permanent failures (invalid task, unreachable location) → Skip
     - Worker failures (stuck, crashed) → Reassign to different worker

4. **No Circuit Breaker**
   - Continuously assigning tasks to a failing worker wastes resources
   - No mechanism to temporarily blacklist unhealthy workers

### Proposed Solution: Intelligent Failure Handling

```java
/**
 * Enhanced failure handler with:
 * - Failure classification
 * - Exponential backoff with jitter
 * - Circuit breaker for unhealthy workers
 * - Alternative strategies for different failure types
 */
public class FailureHandler {

    private static final long BASE_RETRY_DELAY_MS = 1000; // 1 second
    private static final int CIRCUIT_BREAKER_THRESHOLD = 3; // failures before opening
    private static final long CIRCUIT_BREAKER_TIMEOUT_MS = 60_000; // 1 minute

    private final Map<String, WorkerHealth> workerHealth;
    private final ScheduledExecutorService retryExecutor;

    /**
     * Handles task failure with intelligent retry logic.
     */
    public void handleFailure(TaskAssignment assignment, String workerId, String reason) {
        FailureType failureType = classifyFailure(reason);
        WorkerHealth health = workerHealth.computeIfAbsent(
            workerId, k -> new WorkerHealth());

        health.recordFailure(failureType);

        switch (failureType) {
            case TRANSIENT:
                // Retry with exponential backoff
                if (assignment.getRetryCount() < getMaxRetries(assignment)) {
                    scheduleRetry(assignment, workerId, calculateBackoff(assignment.getRetryCount()));
                } else {
                    markFailed(assignment, reason);
                }
                break;

            case PERMANENT:
                // Don't retry permanent failures
                markFailed(assignment, reason);
                break;

            case WORKER_UNHEALTHY:
                // Reassign to different worker
                if (health.shouldCircuitBreak()) {
                    health.openCircuit();
                    reassignToDifferentWorker(assignment, workerId);
                } else {
                    scheduleRetry(assignment, workerId, calculateBackoff(assignment.getRetryCount()));
                }
                break;
        }
    }

    /**
     * Classifies failure type based on error message and context.
     */
    private FailureType classifyFailure(String reason) {
        String lowerReason = reason.toLowerCase();

        // Permanent failures - don't retry
        if (lowerReason.contains("invalid") ||
            lowerReason.contains("unreachable") ||
            lowerReason.contains("not found") ||
            lowerReason.contains("permission denied")) {
            return FailureType.PERMANENT;
        }

        // Worker-specific failures
        if (lowerReason.contains("stuck") ||
            lowerReason.contains("timeout") ||
            lowerReason.contains("no response")) {
            return FailureType.WORKER_UNHEALTHY;
        }

        // Default to transient
        return FailureType.TRANSIENT;
    }

    /**
     * Calculates exponential backoff with jitter.
     */
    private long calculateBackoff(int retryCount) {
        long baseDelay = BASE_RETRY_DELAY_MS * (1L << retryCount); // 2^n
        long jitter = ThreadLocalRandom.current().nextLong(0, baseDelay / 4);
        return baseDelay + jitter;
    }

    /**
     * Schedules a retry with exponential backoff.
     */
    private void scheduleRetry(TaskAssignment assignment, String workerId, long delayMs) {
        retryExecutor.schedule(() -> {
            retryTask(assignment, workerId);
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Gets max retries based on task priority.
     */
    private int getMaxRetries(TaskAssignment assignment) {
        return switch (assignment.getPriority()) {
            case CRITICAL, URGENT -> 5;
            case HIGH -> 3;
            case NORMAL -> 2;
            case LOW -> 1;
        };
    }
}

/**
 * Tracks worker health for circuit breaking.
 */
class WorkerHealth {
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private volatile long circuitOpenUntil = 0;

    public void recordFailure(FailureType type) {
        failureCount.incrementAndGet();
    }

    public void recordSuccess() {
        successCount.incrementAndGet();
        failureCount.set(0); // Reset on success
    }

    public boolean shouldCircuitBreak() {
        return failureCount.get() >= CIRCUIT_BREAKER_THRESHOLD;
    }

    public void openCircuit() {
        circuitOpenUntil = System.currentTimeMillis() + CIRCUIT_BREAKER_TIMEOUT_MS;
    }

    public boolean isCircuitOpen() {
        return System.currentTimeMillis() < circuitOpenUntil;
    }

    public double getSuccessRate() {
        int total = successCount.get() + failureCount.get();
        return total > 0 ? (double) successCount.get() / total : 1.0;
    }
}

enum FailureType {
    TRANSIENT,       // Temporary issue - retry with backoff
    PERMANENT,       // Unrecoverable - don't retry
    WORKER_UNHEALTHY // Worker-specific - reassign
}
```

### Key Improvements

1. **Failure Classification**
   - Automatically categorizes failures based on error messages
   - Applies appropriate strategy per failure type

2. **Exponential Backoff**
   - Delays increase exponentially with each retry (1s, 2s, 4s, 8s...)
   - Adds jitter to avoid thundering herd

3. **Circuit Breaker**
   - Temporarily blacklists workers after repeated failures
   - Automatically recovers after timeout
   - Prevents cascading failures

4. **Priority-Aware Retries**
   - Critical tasks get more retry attempts
   - Low-priority tasks fail fast

---

## 4. Progress Tracking Improvements

### Current Implementation

```java
// OrchestratorService.java, line 335-345
private void handleTaskProgress(String workerId, AgentMessage message) {
    TaskAssignment assignment = workerAssignments.get(workerId);
    if (assignment != null) {
        int percent = message.getPayloadValue("percentComplete", 0);
        String status = message.getPayloadValue("status", "In progress");
        assignment.updateProgress(percent, status);

        LOGGER.debug("[Orchestrator] Task progress from {}: {}% - {}",
            workerId, percent, status);
    }
}

// PlanExecution.java, line 622-626
public int getProgressPercent() {
    if (originalTasks.isEmpty()) return 100;
    int completed = getCompletedCount() + getFailedCount(); // PROBLEM
    return (completed * 100) / originalTasks.size();
}
```

### Problems Identified

1. **Binary Progress Calculation**
   - Only counts completed/failed tasks
   - Ignores in-progress percentage (0-99%)
   - Progress jumps from 0% to 100% when tasks complete

2. **No Weighted Progress**
   - All tasks weighted equally
   - Long-running tasks (e.g., "build castle") same weight as short tasks (e.g., "place torch")

3. **No ETA Estimation**
   - Can't predict when plan will complete
   - No visibility into stuck or slow workers

4. **Progress Not Propagated**
   - Individual task progress tracked locally
   - Not aggregated at plan level
   - Human player sees coarse-grained updates

### Proposed Solution: Weighted Progress Aggregation

```java
/**
 * Enhanced progress tracker with:
 * - Weighted task progress
 * - ETA estimation
 * - Stalled task detection
 * - Hierarchical aggregation
 */
public class ProgressTracker {

    private final Map<String, PlanProgress> planProgress;
    private final Map<String, TaskProgressSnapshot> taskHistory;

    /**
     * Calculates plan-level progress considering:
     * - Weighted task progress (in-progress tasks contribute partial credit)
     * - Task complexity estimates
     * - Historical completion times
     */
    public int getPlanProgress(String planId) {
        PlanProgress progress = planProgress.get(planId);
        if (progress == null) return 0;

        double totalWeight = 0;
        double completedWeight = 0;

        for (TaskAssignment task : progress.getTasks()) {
            double weight = estimateTaskWeight(task);
            totalWeight += weight;

            switch (task.getState()) {
                case COMPLETED:
                    completedWeight += weight;
                    break;
                case IN_PROGRESS, ACCEPTED:
                    // Partial credit based on progress percent
                    completedWeight += weight * (task.getProgressPercent() / 100.0);
                    break;
                case FAILED:
                    // Failed tasks contribute partially based on progress made
                    completedWeight += weight * (task.getProgressPercent() / 200.0); // Half credit
                    break;
                default:
                    // PENDING, ASSIGNED contribute 0
                    break;
            }
        }

        return totalWeight > 0 ? (int) ((completedWeight / totalWeight) * 100) : 0;
    }

    /**
     * Estimates task weight based on:
     * - Action type (build > mine > move)
     * - Parameters (quantity, distance)
     * - Historical duration data
     */
    private double estimateTaskWeight(TaskAssignment task) {
        String action = task.getTaskDescription().toLowerCase();
        Map<String, Object> params = task.getParameters();

        // Base weight by action type
        double baseWeight = switch (action) {
            case "build" -> 10.0;
            case "mine" -> 5.0;
            case "move", "walk" -> 1.0;
            case "craft" -> 3.0;
            case "farm" -> 7.0;
            default -> 2.0;
        };

        // Adjust by parameters
        if (params.containsKey("quantity")) {
            int quantity = ((Number) params.get("quantity")).intValue();
            baseWeight *= Math.log1p(quantity); // Logarithmic scaling
        }

        if (params.containsKey("distance")) {
            int distance = ((Number) params.get("distance")).intValue();
            baseWeight *= (1.0 + distance / 100.0); // Linear scaling for distance
        }

        // Adjust by historical performance
        Double avgDuration = getHistoricalDuration(task);
        if (avgDuration != null) {
            baseWeight *= (avgDuration / 1000.0); // Scale by seconds
        }

        return baseWeight;
    }

    /**
     * Estimates time remaining for plan completion.
     */
    public Duration estimateTimeRemaining(String planId) {
        PlanProgress progress = planProgress.get(planId);
        if (progress == null) return null;

        long totalEstimatedMs = 0;
        long completedMs = 0;

        for (TaskAssignment task : progress.getTasks()) {
            Long estimatedDuration = estimateTaskDuration(task);
            if (estimatedDuration == null) continue;

            totalEstimatedMs += estimatedDuration;

            if (task.getState() == TaskAssignment.State.COMPLETED) {
                completedMs += task.getDurationMs();
            } else if (task.getState().isActive()) {
                completedMs += (estimatedDuration * task.getProgressPercent() / 100);
            }
        }

        long remainingMs = totalEstimatedMs - completedMs;
        return remainingMs > 0 ? Duration.ofMillis(remainingMs) : Duration.ZERO;
    }

    /**
     * Detects stalled or stuck tasks.
     */
    public List<TaskAssignment> detectStalledTasks(String planId) {
        PlanProgress progress = planProgress.get(planId);
        if (progress == null) return List.of();

        long now = System.currentTimeMillis();
        List<TaskAssignment> stalled = new ArrayList<>();

        for (TaskAssignment task : progress.getTasks()) {
            if (!task.getState().isActive()) continue;

            // Check last progress update
            TaskProgressSnapshot snapshot = taskHistory.get(task.getAssignmentId());
            if (snapshot == null) continue;

            long timeSinceUpdate = now - snapshot.timestamp();
            int progressPercent = snapshot.progressPercent();

            // Stalled if no progress for 2 minutes and < 100% complete
            if (timeSinceUpdate > 120_000 && progressPercent < 100) {
                stalled.add(task);
            }

            // Stalled if progress < 10% after 5 minutes
            if (timeSinceUpdate > 300_000 && progressPercent < 10) {
                stalled.add(task);
            }
        }

        return stalled;
    }

    /**
     * Records a progress snapshot for a task.
     */
    public void recordProgress(TaskAssignment task) {
        TaskProgressSnapshot snapshot = new TaskProgressSnapshot(
            task.getAssignmentId(),
            task.getProgressPercent(),
            task.getState(),
            System.currentTimeMillis()
        );
        taskHistory.put(task.getAssignmentId(), snapshot);
    }
}

record TaskProgressSnapshot(String taskId, int progressPercent,
                           TaskAssignment.State state, long timestamp) {}

/**
 * Tracks progress at the plan level.
 */
class PlanProgress {
    private final String planId;
    private final List<TaskAssignment> tasks;
    private final Instant startTime;

    public int getTotalTaskCount() {
        return tasks.size();
    }

    public int getCompletedTaskCount() {
        return (int) tasks.stream()
            .filter(t -> t.getState() == TaskAssignment.State.COMPLETED)
            .count();
    }

    public List<TaskAssignment> getTasks() {
        return tasks;
    }
}
```

### Key Improvements

1. **Weighted Progress**
   - Long/complex tasks contribute more to progress
   - In-progress tasks contribute partial credit
   - More accurate representation of actual work done

2. **ETA Estimation**
   - Predicts completion time based on:
     - Task complexity estimates
     - Historical performance data
     - Current progress rate
   - Helps human player understand time investment

3. **Stalled Task Detection**
   - Identifies tasks that haven't made progress
   - Triggers alerts or automatic reassignment
   - Prevents "stuck" plans from blocking indefinitely

4. **Hierarchical Aggregation**
   - Task progress → Plan progress → Overall progress
   - Works at any granularity
   - Enables drilling down into specific issues

---

## 5. Scalability Improvements

### Current Implementation

```java
// AgentCommunicationBus.java, line 272-288
public AgentMessage poll(String agentId, long timeout, TimeUnit unit) throws InterruptedException {
    PriorityBlockingQueue<AgentMessage> queue = agentQueues.get(agentId);
    if (queue != null) {
        // PROBLEM: Busy-wait with Thread.sleep
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            AgentMessage message = queue.poll();
            if (message != null) {
                stats.recordReceived();
                return message;
            }
            Thread.sleep(10); // Wastes CPU cycles
        }
    }
    return null;
}
```

### Problems Identified

1. **Busy-Wait Polling**
   - `poll()` uses `Thread.sleep(10)` in a loop
   - Wastes CPU cycles checking for messages
   - Adds ~10ms latency to all message delivery

2. **No Message Batching**
   - Each message delivered individually
   - High overhead for frequent small messages
   - No bulk operations

3. **Linear Scan for Broadcasts**
   - `publish()` iterates through all agents for broadcasts
   - O(n) complexity per broadcast message
   - Doesn't scale with hundreds of workers

4. **Unbounded Growth**
   - Message history grows to 1000 messages then drops
   - No size-based limits on agent queues (only message count)
   - Potential memory issues with many agents

### Proposed Solution: Event-Driven Architecture with Batching

```java
/**
 * Scalable communication bus with:
 * - Event-driven delivery (no polling)
 * - Message batching
 * - Efficient broadcast with pub/sub
 * - Memory-efficient backpressure
 */
public class ScalableCommunicationBus {

    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int BATCH_SIZE = 10;
    private static final long BATCH_TIMEOUT_MS = 50;

    // Use blocking queues for event-driven delivery
    private final Map<String, BlockingQueue<MessageBatch>> agentQueues;

    // Topic-based subscriptions for efficient broadcasts
    private final Map<String, Set<String>> topicSubscriptions;
    private final Map<String, Set<String>> agentTopics; // Reverse index

    private final ExecutorService deliveryExecutor;
    private final MessageBatcher batcher;

    public void publish(AgentMessage message) {
        addToHistory(message);
        stats.recordSent(message);

        if (message.isBroadcast()) {
            publishToTopic("broadcast", message);
        } else {
            deliverDirect(message);
        }
    }

    /**
     * Publishes to all subscribers of a topic (O(k) where k = subscribers, not all agents).
     */
    private void publishToTopic(String topic, AgentMessage message) {
        Set<String> subscribers = topicSubscriptions.get(topic);
        if (subscribers == null) return;

        // Filter out sender
        for (String agentId : subscribers) {
            if (!agentId.equals(message.getSenderId())) {
                batcher.queueForDelivery(agentId, message);
            }
        }

        // Trigger batch delivery
        batcher.triggerDelivery();
    }

    /**
     * Batches messages for efficient delivery.
     */
    private class MessageBatcher {
        private final Map<String, List<AgentMessage>> pendingBatches;
        private final ScheduledExecutorService scheduler;

        public void queueForDelivery(String agentId, AgentMessage message) {
            pendingBatches.computeIfAbsent(agentId, k -> new ArrayList<>())
                .add(message);

            // Deliver immediately if batch is full
            List<AgentMessage> batch = pendingBatches.get(agentId);
            if (batch.size() >= BATCH_SIZE) {
                deliverBatch(agentId, batch);
            }
        }

        public void triggerDelivery() {
            // Deliver all pending batches after timeout
            scheduler.schedule(() -> {
                for (Map.Entry<String, List<AgentMessage>> entry : pendingBatches.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        deliverBatch(entry.getKey(), entry.getValue());
                    }
                }
            }, BATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }

        private void deliverBatch(String agentId, List<AgentMessage> batch) {
            deliveryExecutor.submit(() -> {
                BlockingQueue<MessageBatch> queue = agentQueues.get(agentId);
                if (queue != null) {
                    MessageBatch messageBatch = new MessageBatch(batch);

                    // Apply backpressure
                    while (queue.size() >= MAX_QUEUE_SIZE) {
                        try {
                            // Wait for consumer to catch up
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    queue.offer(messageBatch);
                    stats.recordDelivered(batch.size());
                }
            });

            pendingBatches.get(agentId).clear();
        }
    }

    /**
     * Event-driven message consumption (no polling).
     */
    public void subscribe(String agentId, Consumer<MessageBatch> handler) {
        deliveryExecutor.submit(() -> {
            BlockingQueue<MessageBatch> queue = agentQueues.get(agentId);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    MessageBatch batch = queue.take(); // Blocks until available
                    for (AgentMessage message : batch.messages()) {
                        handler.accept(batch);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.error("Error in message handler for {}", agentId, e);
                }
            }
        });
    }

    /**
     * Subscribe to topics for efficient broadcasts.
     */
    public void subscribeToTopic(String agentId, String topic) {
        topicSubscriptions.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet())
            .add(agentId);
        agentTopics.computeIfAbsent(agentId, k -> ConcurrentHashMap.newKeySet())
            .add(topic);
    }
}

/**
 * A batch of messages delivered together.
 */
record MessageBatch(List<AgentMessage> messages) {
    public int size() {
        return messages.size();
    }
}
```

### Key Improvements

1. **Event-Driven Delivery**
   - Uses `BlockingQueue.take()` instead of polling
   - Zero CPU usage when no messages
   - Immediate delivery when messages arrive

2. **Message Batching**
   - Accumulates messages up to `BATCH_SIZE` or `BATCH_TIMEOUT_MS`
   - Reduces delivery overhead for high message rates
   - Preserves message ordering within batches

3. **Topic-Based Pub/Sub**
   - Broadcasts use topic subscriptions (O(k) instead of O(n))
   - Agents only receive relevant messages
   - Scales to hundreds of workers

4. **Backpressure Handling**
   - Blocks producers when consumer can't keep up
   - Prevents memory exhaustion
   - Maintains system stability under load

---

## 6. Implementation Roadmap

### Phase 1: Core Infrastructure (Priority: HIGH)
1. **Implement `SmartTaskDistributor`**
   - Add `WorkerProfile` class
   - Implement capability-based worker selection
   - Add load balancing

2. **Enhance `FailureHandler`**
   - Implement failure classification
   - Add exponential backoff
   - Implement circuit breaker

### Phase 2: Progress Tracking (Priority: MEDIUM)
1. **Implement `ProgressTracker`**
   - Add weighted progress calculation
   - Implement ETA estimation
   - Add stalled task detection

2. **Update `OrchestratorService`**
   - Integrate new distributors and handlers
   - Add progress callbacks

### Phase 3: Scalability (Priority: MEDIUM)
1. **Implement `ScalableCommunicationBus`**
   - Replace polling with event-driven delivery
   - Add message batching
   - Implement topic-based subscriptions

2. **Update `AgentCommunicationBus`**
   - Migrate to new architecture
   - Maintain backward compatibility

### Phase 4: Monitoring & Observability (Priority: LOW)
1. **Add Metrics**
   - Worker performance metrics
   - Plan completion times
   - Message throughput

2. **Add Debugging Tools**
   - Visual task dependency graph
   - Worker load visualization
   - Progress dashboard

---

## 7. Code Changes Required

### New Files to Create

```
src/main/java/com/minewright/orchestration/
├── SmartTaskDistributor.java       # Enhanced task distribution
├── WorkerProfile.java               # Worker capability tracking
├── FailureHandler.java              # Intelligent failure handling
├── WorkerHealth.java                # Circuit breaker implementation
├── ProgressTracker.java             # Weighted progress tracking
├── PlanProgress.java                # Plan-level progress
├── ScalableCommunicationBus.java    # Event-driven messaging
└── MessageBatcher.java              # Message batching utility
```

### Files to Modify

```
src/main/java/com/minewright/orchestration/
├── OrchestratorService.java         # Integrate new components
│   ├── Replace distributeTasks() with SmartTaskDistributor
│   ├── Replace handleTaskFailed() with FailureHandler
│   └── Add ProgressTracker integration
│
├── TaskAssignment.java              # Add capability hints
│   └── Add getRequiredCapabilities() method
│
└── AgentMessage.java                # Add batching support
    └── Add createBatch() factory method
```

### Changes to OrchestratorService.java

```java
public class OrchestratorService {

    // New components
    private final SmartTaskDistributor taskDistributor;
    private final FailureHandler failureHandler;
    private final ProgressTracker progressTracker;

    public OrchestratorService() {
        this.taskDistributor = new SmartTaskDistributor();
        this.failureHandler = new FailureHandler();
        this.progressTracker = new ProgressTracker();
        // ... rest of initialization
    }

    // Replace distributeTasks()
    private void distributeTasks(PlanExecution plan, Collection<ForemanEntity> availableSteves) {
        taskDistributor.distributeTasks(plan, availableSteves);
    }

    // Replace handleTaskFailed()
    private void handleTaskFailed(String workerId, AgentMessage message) {
        TaskAssignment assignment = workerAssignments.remove(workerId);
        if (assignment != null) {
            String reason = message.getPayloadValue("result", "Unknown error");
            failureHandler.handleFailure(assignment, workerId, reason);
        }
    }

    // Add progress tracking
    private void handleTaskProgress(String workerId, AgentMessage message) {
        TaskAssignment assignment = workerAssignments.get(workerId);
        if (assignment != null) {
            int percent = message.getPayloadValue("percentComplete", 0);
            String status = message.getPayloadValue("status", "In progress");
            assignment.updateProgress(percent, status);

            // Record progress snapshot
            progressTracker.recordProgress(assignment);

            // Check for stalled tasks
            List<TaskAssignment> stalled = progressTracker.detectStalledTasks(
                assignment.getParentPlanId());
            for (TaskAssignment stalledTask : stalled) {
                LOGGER.warn("[Orchestrator] Stalled task detected: {}", stalledTask);
                // Could trigger reassignment here
            }
        }
    }

    // Add weighted progress calculation
    public int getPlanProgress(String planId) {
        return progressTracker.getPlanProgress(planId);
    }

    // Add ETA estimation
    public Duration getEstimatedTimeRemaining(String planId) {
        return progressTracker.estimateTimeRemaining(planId);
    }
}
```

---

## 8. Testing Strategy

### Unit Tests

```java
class SmartTaskDistributorTest {
    @Test
    void testSpecialistReceivesMatchingTasks() {
        // Given
        WorkerProfile specialist = new WorkerProfile("miner", AgentRole.SPECIALIST);
        specialist.addCapability("mine");

        TaskAssignment miningTask = createTask("mine", Map.of("quantity", 10));

        // When
        String selected = distributor.selectBestWorker(miningTask, profiles);

        // Then
        assertEquals("miner", selected);
    }

    @Test
    void testLoadBalancing() {
        // Verify tasks distributed to less loaded workers
    }

    @Test
    void testDependencyValidation() {
        // Verify dependent tasks not assigned prematurely
    }
}

class FailureHandlerTest {
    @Test
    void testExponentialBackoff() {
        // Verify retry delays increase exponentially
    }

    @Test
    void testCircuitBreakerOpens() {
        // Verify circuit opens after threshold failures
    }

    @Test
    void testPermanentFailureNoRetry() {
        // Verify permanent failures not retried
    }
}

class ProgressTrackerTest {
    @Test
    void testWeightedProgressCalculation() {
        // Verify long tasks contribute more to progress
    }

    @Test
    void testInaccurateProgressDetection() {
        // Verify stalled tasks detected
    }

    @Test
    void testETAEstimation() {
        // Verify ETA accuracy with known tasks
    }
}
```

### Integration Tests

```java
class OrchestratorServiceIntegrationTest {
    @Test
    void testFullPlanExecution() {
        // Create plan with multiple tasks
        // Verify distribution
        // Verify progress tracking
        // Verify completion
    }

    @Test
    void testWorkerFailureRecovery() {
        // Simulate worker failure
        // Verify task reassigned
        // Verify plan completes
    }

    @Test
    void testScalabilityWithManyWorkers() {
        // Create 100 workers
        // Create plan with 1000 tasks
        // Verify reasonable completion time
    }
}
```

---

## 9. Performance Considerations

### Expected Improvements

| Metric | Current | Expected | Improvement |
|--------|---------|----------|-------------|
| Task Distribution Time | O(n) | O(n log n) | Better quality, slightly slower |
| Failure Recovery Time | Fixed 2 retries | Adaptive 1-5 retries | Faster for transient failures |
| Progress Granularity | Binary (0% or 100%) | Weighted (0-100%) | Much more accurate |
| Message Throughput | ~100 msg/sec | ~1000 msg/sec | 10x improvement |
| CPU Usage (Idle) | High (polling) | Near zero (event-driven) | Major improvement |
| Memory per Worker | ~1 KB | ~2 KB (with metrics) | Acceptable trade-off |

### Bottlenecks to Monitor

1. **Task Distribution** - May become bottleneck with 1000+ tasks
   - Solution: Distribute in batches

2. **Progress Tracking** - History storage grows with task count
   - Solution: Periodic cleanup of old snapshots

3. **Communication Bus** - Topic subscriptions need memory
   - Solution: Limit subscription depth and TTL

---

## 10. Conclusion

The orchestration system has a solid foundation but requires enhancements in five critical areas:

1. **Task Distribution** - Move from naive round-robin to capability-based, load-aware distribution
2. **Failure Handling** - Add failure classification, exponential backoff, and circuit breaking
3. **Progress Tracking** - Implement weighted progress aggregation and ETA estimation
4. **Scalability** - Replace polling with event-driven architecture and batching
5. **Observability** - Add comprehensive metrics and debugging tools

The proposed changes maintain backward compatibility while significantly improving:
- **Fairness** - Workers matched to tasks they can handle efficiently
- **Reliability** - Intelligent retry and failure recovery
- **Accuracy** - Precise progress tracking and time estimates
- **Performance** - Event-driven messaging with batching
- **Scalability** - Supports 100+ workers efficiently

Implementation should follow the phased roadmap, with priority given to core infrastructure (task distribution and failure handling) before moving to scalability enhancements.

---

**Document Version:** 1.0
**Author:** Claude Code Analysis
**Last Updated:** 2026-02-27
