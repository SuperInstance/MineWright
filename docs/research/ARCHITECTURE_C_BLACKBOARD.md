# Architecture C: Blackboard Pattern for Foreman AI

## Executive Summary

The **Blackboard Architecture** is an AI pattern where multiple independent specialists (knowledge sources) observe a shared data structure (the blackboard) and contribute to solving a problem collaboratively. For the MineWright foreman system, this pattern enables multiple AI specialists to coordinate tasks, maintain conversations, and manage the crew team through a shared knowledge base.

**Current Status:** The MineWright codebase already implements elements of this pattern through `OrchestratorService`, `AgentCommunicationBus`, and `CompanionMemory`. This document outlines how to fully realize the blackboard pattern with distinct knowledge sources.

---

## 1. Overview

### 1.1 How the Blackboard Pattern Works

```
┌──────────────────────────────────────────────────────────────┐
│                        BLACKBOARD                             │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌───────────┐ │
│  │    Tasks   │ │  Dialogue  │ │ World State│ │ Workers   │ │
│  │    Queue   │ │  History   │ │  Knowledge │ │  Status   │ │
│  └────────────┘ └────────────┘ └────────────┘ └───────────┘ │
└──────────────────────────────────────────────────────────────┘
         ▲              ▲              ▲              ▲
         │              │              │              │
         │ inspect      │ inspect      │ inspect      │ inspect
         │ contribute   │ contribute   │ contribute   │ contribute
         │              │              │              │
┌────────┴──────┐ ┌─────┴──────┐ ┌───┴──────────┐ ┌─┴──────────┐
│   PLANNER     │ │CONVERSATION│ │  MEMORY      │ │COORDINATOR │
│  Specialist   │ │ALIST       │ │  KEEPER      │ │Specialist  │
│               │ │            │ │              │ │            │
│ - Plan tasks  │ │ - Chat w/  │ │ - Store     │ │ - Assign   │
│ - Break goals │ │   player   │ │   memories  │ │   workers  │
│ - Validate    │ │ - Tell     │ │ - Retrieve  │ │ - Monitor  │
│   actions     │ │   jokes    │ │   relevant  │ │   progress │
└───────────────┘ └────────────┘ └──────────────┘ └────────────┘
```

### 1.2 Key Characteristics

- **Shared Knowledge Base:** All specialists read from and write to the blackboard
- **Independent Specialists:** Each knowledge source operates autonomously
- **Opportunistic Problem Solving:** Specialists act when they can contribute
- **Incremental Solution:** Solution builds up over time through contributions
- **No Direct Communication:** Specialists communicate only through the blackboard

### 1.3 How It Differs from Other Patterns

| Pattern | Communication | Coordination | Best For |
|---------|---------------|--------------|----------|
| **Blackboard** | Shared state | Independent opportunistic | Complex, multi-faceted problems |
| **Pipeline** | Message passing | Sequential stages | Transformations with clear stages |
| **Pub-Sub** | Event broadcast | Event-driven reactions | Loose coupling, event handling |
| **Orchestrator** | Central coordinator | Explicit control | Complex workflows with dependencies |

---

## 2. Blackboard Data Structure

### 2.1 Core Blackboard Interface

```java
package com.minewright.blackboard;

import com.minewright.orchestration.AgentMessage;
import com.minewright.action.Task;
import com.minewright.memory.CompanionMemory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Central blackboard for foreman AI coordination.
 *
 * <p>The blackboard is a shared data structure that all knowledge sources
 * inspect and modify. It contains the current state of the foreman's world,
 * including tasks, conversations, worker status, and memories.</p>
 *
 * <p><b>Thread Safety:</b> All operations are thread-safe using concurrent collections.</p>
 *
 * @since 1.4.0
 */
public interface Blackboard {

    /**
     * Registers a knowledge source with the blackboard.
     *
     * @param source The knowledge source to register
     */
    void registerSource(KnowledgeSource source);

    /**
     * Unregisters a knowledge source from the blackboard.
     *
     * @param sourceId The ID of the source to unregister
     */
    void unregisterSource(String sourceId);

    /**
     * Gets all currently registered knowledge sources.
     *
     * @return Set of registered source IDs
     */
    Set<String> getRegisteredSources();

    // === Task Management ===

    /**
     * Adds a new task to the blackboard.
     *
     * @param task The task to add
     * @param sourceId The ID of the source adding this task
     */
    void addTask(Task task, String sourceId);

    /**
     * Updates an existing task on the blackboard.
     *
     * @param taskId The ID of the task to update
     * @param updatedTask The updated task data
     */
    void updateTask(String taskId, Task updatedTask);

    /**
     * Removes a task from the blackboard.
     *
     * @param taskId The ID of the task to remove
     */
    void removeTask(String taskId);

    /**
     * Gets all tasks currently on the blackboard.
     *
     * @return Unmodifiable list of tasks
     */
    List<Task> getAllTasks();

    /**
     * Gets tasks filtered by state.
     *
     * @param state The task state to filter by
     * @return List of tasks in the given state
     */
    List<Task> getTasksByState(Task.State state);

    /**
     * Gets a specific task by ID.
     *
     * @param taskId The task ID
     * @return Optional containing the task, or empty if not found
     */
    Optional<Task> getTask(String taskId);

    // === Worker Management ===

    /**
     * Registers a worker MineWright with the blackboard.
     *
     * @param workerId The worker's ID
     * @param workerName The worker's display name
     * @param capabilities The worker's capabilities
     */
    void registerWorker(String workerId, String workerName, WorkerCapabilities capabilities);

    /**
     * Unregisters a worker from the blackboard.
     *
     * @param workerId The worker's ID
     */
    void unregisterWorker(String workerId);

    /**
     * Updates a worker's current status.
     *
     * @param workerId The worker's ID
     * @param status The new status
     */
    void updateWorkerStatus(String workerId, WorkerStatus status);

    /**
     * Gets all registered workers.
     *
     * @return Map of worker ID to worker info
     */
    Map<String, WorkerInfo> getAllWorkers();

    /**
     * Gets available workers (not currently assigned).
     *
     * @return List of available worker IDs
     */
    List<String> getAvailableWorkers();

    /**
     * Gets workers with a specific capability.
     *
     * @param capability The required capability
     * @return List of worker IDs with this capability
     */
    List<String> getWorkersWithCapability(String capability);

    // === Conversation Management ===

    /**
     * Adds a message to the conversation history.
     *
     * @param message The message to add
     */
    void addConversationMessage(ConversationMessage message);

    /**
     * Gets recent conversation history.
     *
     * @param count Maximum number of messages to retrieve
     * @return List of recent messages
     */
    List<ConversationMessage> getConversationHistory(int count);

    /**
     * Sets the current conversation topic.
     *
     * @param topic The current topic
     */
    void setCurrentTopic(String topic);

    /**
     * Gets the current conversation topic.
     *
     * @return The current topic, or null if none
     */
    String getCurrentTopic();

    /**
     * Checks if a topic was recently discussed.
     *
     * @param topic The topic to check
     * @return true if discussed recently
     */
    boolean wasRecentlyDiscussed(String topic);

    // === World State ===

    /**
     * Updates the world state information.
     *
     * @param state The new world state
     */
    void updateWorldState(WorldState state);

    /**
     * Gets the current world state.
     *
     * @return The current world state
     */
    WorldState getWorldState();

    /**
     * Gets a specific fact from the world state.
     *
     * @param key The fact key
     * @return Optional containing the fact, or empty if not found
     */
    Optional<Object> getWorldFact(String key);

    // === Memory Access ===

    /**
     * Gets the companion memory for relationship data.
     *
     * @return The companion memory
     */
    CompanionMemory getCompanionMemory();

    /**
     * Records a shared experience.
     *
     * @param eventType The type of event
     * @param description What happened
     * @param emotionalWeight Emotional significance (-10 to +10)
     */
    void recordExperience(String eventType, String description, int emotionalWeight);

    /**
     * Finds relevant memories.
     *
     * @param query Query to search for
     * @param k Maximum number of results
     * @return List of relevant memories
     */
    List<CompanionMemory.EpisodicMemory> findRelevantMemories(String query, int k);

    // === Coordination ===

    /**
     * Assigns a task to a worker.
     *
     * @param taskId The task to assign
     * @param workerId The worker to assign to
     */
    void assignTask(String taskId, String workerId);

    /**
     * Records task progress.
     *
     * @param taskId The task ID
     * @param progress Progress percentage (0-100)
     * @param status Status message
     */
    void recordTaskProgress(String taskId, int progress, String status);

    /**
     * Marks a task as complete.
     *
     * @param taskId The task ID
     * @param result Completion result
     */
    void completeTask(String taskId, String result);

    /**
     * Marks a task as failed.
     *
     * @param taskId The task ID
     * @param reason Failure reason
     */
    void failTask(String taskId, String reason);

    // === Events and Notifications ===

    /**
     * Adds a blackboard event listener.
     *
     * @param listener The listener to add
     */
    void addListener(BlackboardListener listener);

    /**
     * Removes a blackboard event listener.
     *
     * @param listener The listener to remove
     */
    void removeListener(BlackboardListener listener);

    /**
     * Triggers a blackboard scan, notifying all registered sources.
     * Called when significant state changes occur.
     */
    void triggerScan();

    // === Debugging and Inspection ===

    /**
     * Gets a snapshot of the current blackboard state.
     *
     * @return BlackboardSnapshot containing current state
     */
    BlackboardSnapshot getSnapshot();

    /**
     * Gets contribution history for debugging.
     *
     * @return List of recent contributions
     */
    List<Contribution> getContributionHistory(int count);

    /**
     * Clears all data from the blackboard.
     */
    void clear();
}
```

### 2.2 Implementation

```java
package com.minewright.blackboard;

import com.minewright.MineWrightMod;
import com.minewright.action.Task;
import com.minewright.memory.CompanionMemory;
import com.minewright.orchestration.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Thread-safe implementation of the Blackboard interface.
 *
 * <p>Uses concurrent collections for safe access from multiple knowledge sources.</p>
 */
public class ForemanBlackboard implements Blackboard {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForemanBlackboard.class);

    // Maximum history sizes
    private static final int MAX_CONVERSATION_HISTORY = 100;
    private static final int MAX_CONTRIBUTION_HISTORY = 500;
    private static final int MAX_TASKS = 1000;

    // Registered knowledge sources
    private final Map<String, KnowledgeSource> knowledgeSources;

    // Task management
    private final Map<String, Task> tasks;
    private final Map<String, Task.State> taskStates;
    private final Map<String, String> taskAssignments;  // taskId -> workerId
    private final Map<String, TaskProgress> taskProgress;

    // Worker management
    private final Map<String, WorkerInfo> workers;
    private final Map<String, WorkerStatus> workerStatuses;

    // Conversation
    private final List<ConversationMessage> conversationHistory;
    private volatile String currentTopic;
    private final Set<String> recentTopics;

    // World state
    private volatile WorldState worldState;

    // Memory
    private final CompanionMemory companionMemory;

    // Coordination
    private final Map<String, Instant> workerLastSeen;

    // Event listeners
    private final List<BlackboardListener> listeners;

    // Contribution tracking
    private final Queue<Contribution> contributionHistory;
    private final AtomicLong contributionCounter;

    // Scan scheduling
    private final ScheduledExecutorService scanScheduler;
    private volatile boolean scanPending = false;

    public ForemanBlackboard(CompanionMemory companionMemory) {
        this.knowledgeSources = new ConcurrentHashMap<>();
        this.tasks = new ConcurrentHashMap<>();
        this.taskStates = new ConcurrentHashMap<>();
        this.taskAssignments = new ConcurrentHashMap<>();
        this.taskProgress = new ConcurrentHashMap<>();

        this.workers = new ConcurrentHashMap<>();
        this.workerStatuses = new ConcurrentHashMap<>();

        this.conversationHistory = new LinkedBlockingDeque<>(MAX_CONVERSATION_HISTORY);
        this.currentTopic = null;
        this.recentTopics = ConcurrentHashMap.newKeySet();

        this.worldState = new WorldState();
        this.companionMemory = companionMemory;

        this.workerLastSeen = new ConcurrentHashMap<>();

        this.listeners = new CopyOnWriteArrayList<>();

        this.contributionHistory = new LinkedBlockingDeque<>(MAX_CONTRIBUTION_HISTORY);
        this.contributionCounter = new AtomicLong(0);

        this.scanScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "Blackboard-Scanner");
            thread.setDaemon(true);
            return thread;
        });

        LOGGER.info("ForemanBlackboard initialized");
    }

    @Override
    public void registerSource(KnowledgeSource source) {
        knowledgeSources.put(source.getId(), source);
        source.setBlackboard(this);

        LOGGER.info("Registered knowledge source: {}", source.getId());
    }

    @Override
    public void unregisterSource(String sourceId) {
        KnowledgeSource removed = knowledgeSources.remove(sourceId);
        if (removed != null) {
            removed.setBlackboard(null);
            LOGGER.info("Unregistered knowledge source: {}", sourceId);
        }
    }

    @Override
    public Set<String> getRegisteredSources() {
        return Collections.unmodifiableSet(knowledgeSources.keySet());
    }

    // Task management methods...

    @Override
    public void addTask(Task task, String sourceId) {
        String taskId = task.getAction() + "_" + contributionCounter.incrementAndGet();
        tasks.put(taskId, task);
        taskStates.put(taskId, Task.State.PENDING);

        recordContribution(sourceId, "ADD_TASK", "Added task: " + task.getAction());
        notifyListeners(BlackboardEvent.taskAdded(taskId, task));
    }

    @Override
    public void updateTask(String taskId, Task updatedTask) {
        if (tasks.containsKey(taskId)) {
            tasks.put(taskId, updatedTask);
            notifyListeners(BlackboardEvent.taskUpdated(taskId, updatedTask));
        }
    }

    @Override
    public void removeTask(String taskId) {
        Task removed = tasks.remove(taskId);
        if (removed != null) {
            taskStates.remove(taskId);
            taskAssignments.remove(taskId);
            taskProgress.remove(taskId);
            notifyListeners(BlackboardEvent.taskRemoved(taskId));
        }
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Task> getTasksByState(Task.State state) {
        return tasks.entrySet().stream()
            .filter(e -> taskStates.getOrDefault(e.getKey(), Task.State.PENDING) == state)
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Task> getTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    // Worker management methods...

    @Override
    public void registerWorker(String workerId, String workerName, WorkerCapabilities capabilities) {
        workers.put(workerId, new WorkerInfo(workerId, workerName, capabilities));
        workerStatuses.put(workerId, WorkerStatus.AVAILABLE);

        LOGGER.info("Registered worker: {} ({})", workerName, workerId);
        notifyListeners(BlackboardEvent.workerRegistered(workerId, workerName));
    }

    @Override
    public void unregisterWorker(String workerId) {
        WorkerInfo removed = workers.remove(workerId);
        if (removed != null) {
            workerStatuses.remove(workerId);
            // Reassign any tasks this worker had
            reassignWorkerTasks(workerId);
            notifyListeners(BlackboardEvent.workerUnregistered(workerId));
        }
    }

    @Override
    public void updateWorkerStatus(String workerId, WorkerStatus status) {
        workerStatuses.put(workerId, status);
        workerLastSeen.put(workerId, Instant.now());
        notifyListeners(BlackboardEvent.workerStatusChanged(workerId, status));
    }

    @Override
    public Map<String, WorkerInfo> getAllWorkers() {
        return Collections.unmodifiableMap(workers);
    }

    @Override
    public List<String> getAvailableWorkers() {
        return workers.entrySet().stream()
            .filter(e -> workerStatuses.getOrDefault(e.getKey(), WorkerStatus.AVAILABLE) == WorkerStatus.AVAILABLE)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getWorkersWithCapability(String capability) {
        return workers.entrySet().stream()
            .filter(e -> e.getValue().capabilities().hasCapability(capability))
            .filter(e -> workerStatuses.getOrDefault(e.getKey(), WorkerStatus.AVAILABLE) == WorkerStatus.AVAILABLE)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    // Conversation methods...

    @Override
    public void addConversationMessage(ConversationMessage message) {
        while (((LinkedBlockingDeque<ConversationMessage>) conversationHistory).size() >= MAX_CONVERSATION_HISTORY) {
            conversationHistory.remove(conversationHistory.size() - 1);
        }
        conversationHistory.add(0, message);

        if (message.topic() != null) {
            currentTopic = message.topic();
            recentTopics.add(message.topic().toLowerCase());
        }

        notifyListeners(BlackboardEvent.conversationAdded(message));
    }

    @Override
    public List<ConversationMessage> getConversationHistory(int count) {
        return conversationHistory.stream()
            .limit(count)
            .collect(Collectors.toList());
    }

    @Override
    public String getCurrentTopic() {
        return currentTopic;
    }

    @Override
    public boolean wasRecentlyDiscussed(String topic) {
        return recentTopics.contains(topic.toLowerCase());
    }

    // World state methods...

    @Override
    public void updateWorldState(WorldState state) {
        this.worldState = state;
        notifyListeners(BlackboardEvent.worldStateChanged(state));
    }

    @Override
    public WorldState getWorldState() {
        return worldState;
    }

    @Override
    public Optional<Object> getWorldFact(String key) {
        return Optional.ofNullable(worldState.facts().get(key));
    }

    // Memory methods...

    @Override
    public CompanionMemory getCompanionMemory() {
        return companionMemory;
    }

    @Override
    public void recordExperience(String eventType, String description, int emotionalWeight) {
        companionMemory.recordExperience(eventType, description, emotionalWeight);
        notifyListeners(BlackboardEvent.memoryRecorded(eventType, description));
    }

    @Override
    public List<CompanionMemory.EpisodicMemory> findRelevantMemories(String query, int k) {
        return companionMemory.findRelevantMemories(query, k);
    }

    // Coordination methods...

    @Override
    public void assignTask(String taskId, String workerId) {
        taskAssignments.put(taskId, workerId);
        taskStates.put(taskId, Task.State.ASSIGNED);
        workerStatuses.put(workerId, WorkerStatus.BUSY);

        LOGGER.info("Assigned task {} to worker {}", taskId, workerId);
        notifyListeners(BlackboardEvent.taskAssigned(taskId, workerId));
    }

    @Override
    public void recordTaskProgress(String taskId, int progress, String status) {
        taskProgress.put(taskId, new TaskProgress(progress, status, Instant.now()));
        notifyListeners(BlackboardEvent.taskProgress(taskId, progress));
    }

    @Override
    public void completeTask(String taskId, String result) {
        String workerId = taskAssignments.get(taskId);
        if (workerId != null) {
            workerStatuses.put(workerId, WorkerStatus.AVAILABLE);
        }

        taskStates.put(taskId, Task.State.COMPLETED);
        recordContribution("SYSTEM", "TASK_COMPLETE", "Task completed: " + result);

        LOGGER.info("Task {} completed: {}", taskId, result);
        notifyListeners(BlackboardEvent.taskCompleted(taskId, result));
    }

    @Override
    public void failTask(String taskId, String reason) {
        String workerId = taskAssignments.get(taskId);
        if (workerId != null) {
            workerStatuses.put(workerId, WorkerStatus.AVAILABLE);
        }

        taskStates.put(taskId, Task.State.FAILED);
        recordContribution("SYSTEM", "TASK_FAILED", "Task failed: " + reason);

        LOGGER.warn("Task {} failed: {}", taskId, reason);
        notifyListeners(BlackboardEvent.taskFailed(taskId, reason));
    }

    // Event listeners...

    @Override
    public void addListener(BlackboardListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(BlackboardListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void triggerScan() {
        if (scanPending) {
            return;  // Scan already scheduled
        }

        scanPending = true;

        // Schedule scan to run asynchronously
        scanScheduler.submit(() -> {
            try {
                performScan();
            } finally {
                scanPending = false;
            }
        });
    }

    private void performScan() {
        LOGGER.debug("Performing blackboard scan...");

        for (KnowledgeSource source : knowledgeSources.values()) {
            try {
                if (source.canContribute()) {
                    source.evaluate();
                }
            } catch (Exception e) {
                LOGGER.error("Error during scan for source {}", source.getId(), e);
            }
        }
    }

    // Snapshot and history...

    @Override
    public BlackboardSnapshot getSnapshot() {
        return new BlackboardSnapshot(
            new HashMap<>(tasks),
            new HashMap<>(taskStates),
            new HashMap<>(workers),
            new ArrayList<>(conversationHistory),
            worldState,
            new HashMap<>(workerStatuses)
        );
    }

    @Override
    public List<Contribution> getContributionHistory(int count) {
        return contributionHistory.stream()
            .limit(count)
            .collect(Collectors.toList());
    }

    @Override
    public void clear() {
        tasks.clear();
        taskStates.clear();
        taskAssignments.clear();
        taskProgress.clear();
        workers.clear();
        workerStatuses.clear();
        conversationHistory.clear();
        recentTopics.clear();
        currentTopic = null;
        contributionHistory.clear();

        LOGGER.info("Blackboard cleared");
    }

    // Private helper methods...

    private void recordContribution(String sourceId, String type, String description) {
        Contribution contribution = new Contribution(
            contributionCounter.incrementAndGet(),
            sourceId,
            type,
            description,
            Instant.now()
        );

        while (contributionHistory.size() >= MAX_CONTRIBUTION_HISTORY) {
            contributionHistory.remove();
        }
        contributionHistory.offer(contribution);
    }

    private void notifyListeners(BlackboardEvent event) {
        for (BlackboardListener listener : listeners) {
            try {
                listener.onBlackboardEvent(event);
            } catch (Exception e) {
                LOGGER.error("Error notifying listener", e);
            }
        }
    }

    private void reassignWorkerTasks(String workerId) {
        // Find all tasks assigned to this worker
        List<String> tasksToReassign = taskAssignments.entrySet().stream()
            .filter(e -> e.getValue().equals(workerId))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Mark them as pending for reassignment
        for (String taskId : tasksToReassign) {
            taskAssignments.remove(taskId);
            taskStates.put(taskId, Task.State.PENDING);
        }

        if (!tasksToReassign.isEmpty()) {
            LOGGER.info("Reassigning {} tasks from worker {}", tasksToReassign.size(), workerId);
            triggerScan();  // Trigger reassignment
        }
    }

    public void shutdown() {
        scanScheduler.shutdown();
        try {
            if (!scanScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scanScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scanScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info("ForemanBlackboard shut down");
    }
}
```

---

## 3. Knowledge Sources

### 3.1 Knowledge Source Interface

```java
package com.minewright.blackboard;

/**
 * Interface for blackboard knowledge sources.
 *
 * <p>Knowledge sources inspect the blackboard and contribute solutions
 * when they can help solve the current problem.</p>
 *
 * @since 1.4.0
 */
public interface KnowledgeSource {

    /**
     * Gets the unique ID of this knowledge source.
     *
     * @return The source ID
     */
    String getId();

    /**
     * Gets the priority of this source (higher = evaluated first).
     *
     * @return Priority value (0-100)
     */
    int getPriority();

    /**
     * Sets the blackboard for this source.
     *
     * @param blackboard The blackboard
     */
    void setBlackboard(Blackboard blackboard);

    /**
     * Checks if this source can contribute to the current state.
     *
     * @return true if this source should evaluate
     */
    boolean canContribute();

    /**
     * Evaluates the blackboard and contributes if able.
     * Called when canContribute() returns true.
     */
    void evaluate();

    /**
     * Gets the current confidence level of this source.
     *
     * @return Confidence (0.0 to 1.0)
     */
    double getConfidence();

    /**
     * Resets the source's internal state.
     */
    void reset();
}
```

### 3.2 Planner Specialist

```java
package com.minewright.blackboard.specialists;

import com.minewright.MineWrightMod;
import com.minewright.blackboard.Blackboard;
import com.minewright.blackboard.KnowledgeSource;
import com.minewright.action.Task;
import com.minewright.llm.ResponseParser;
import com.minewright.llm.TaskPlanner;
import com.minewright.entity.ForemanEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Planner Specialist - breaks down goals into executable tasks.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Receives high-level commands from player</li>
 *   <li>Plans task sequences using LLM</li>
 *   <li>Validates generated tasks</li>
 *   <li>Adds tasks to blackboard</li>
 * </ul>
 */
public class PlannerSpecialist implements KnowledgeSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlannerSpecialist.class);

    private static final int ID = "planner";
    private static final int PRIORITY = 90;  // High priority

    private Blackboard blackboard;
    private final ForemanEntity foremanEntity;
    private final TaskPlanner taskPlanner;

    private volatile boolean isPlanning = false;
    private CompletableFuture<ResponseParser.ParsedResponse> currentPlan;

    public PlannerSpecialist(ForemanEntity foremanEntity) {
        this.foremanEntity = foremanEntity;
        this.taskPlanner = new TaskPlanner();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void setBlackboard(Blackboard blackboard) {
        this.blackboard = blackboard;
    }

    @Override
    public boolean canContribute() {
        // Can contribute if:
        // 1. Not already planning
        // 2. There's a pending command to process
        // 3. There's capacity for more tasks
        return !isPlanning
            && blackboard.getCurrentTopic() != null
            && blackboard.getCurrentTopic().startsWith("cmd:")
            && blackboard.getAllTasks().size() < 50;
    }

    @Override
    public void evaluate() {
        String topic = blackboard.getCurrentTopic();
        if (topic == null || !topic.startsWith("cmd:")) {
            return;
        }

        String command = topic.substring(4);  // Remove "cmd:" prefix
        LOGGER.info("[Planner] Planning tasks for command: {}", command);

        isPlanning = true;

        // Start async planning
        currentPlan = taskPlanner.planTasksAsync(foremanEntity, command);

        // Non-blocking check in evaluate
        currentPlan.thenAccept(parsedResponse -> {
            if (parsedResponse != null) {
                // Validate tasks
                List<Task> validTasks = taskPlanner.validateAndFilterTasks(parsedResponse.getTasks());

                LOGGER.info("[Planner] Generated {} valid tasks for: {}",
                    validTasks.size(), parsedResponse.getPlan());

                // Add tasks to blackboard
                for (Task task : validTasks) {
                    blackboard.addTask(task, getId());
                }

                // Record experience
                blackboard.recordExperience(
                    "task_planning",
                    "Planned: " + parsedResponse.getPlan(),
                    2
                );

                // Clear the command topic
                blackboard.setCurrentTopic(null);

                // Trigger scan for other specialists
                blackboard.triggerScan();
            } else {
                LOGGER.warn("[Planner] Failed to generate plan for: {}", command);

                // Add error message to conversation
                blackboard.addConversationMessage(new ConversationMessage(
                    Instant.now(),
                    "foreman",
                    "player",
                    "I'm sorry, I couldn't understand that command. Could you rephrase it?",
                    "error",
                    null
                ));

                blackboard.setCurrentTopic(null);
            }

            isPlanning = false;
        }).exceptionally(error -> {
            LOGGER.error("[Planner] Error during planning", error);
            isPlanning = false;
            return null;
        });
    }

    @Override
    public double getConfidence() {
        // Confidence based on:
        // - Recent success rate
        // - Current world knowledge quality
        // - Command clarity
        return 0.8;  // TODO: Calculate based on history
    }

    @Override
    public void reset() {
        if (currentPlan != null) {
            currentPlan.cancel(false);
            currentPlan = null;
        }
        isPlanning = false;
    }
}
```

### 3.3 Conversationalist Specialist

```java
package com.minewright.blackboard.specialists;

import com.minewright.MineWrightMod;
import com.minewright.blackboard.Blackboard;
import com.minewright.blackboard.ConversationMessage;
import com.minewright.blackboard.KnowledgeSource;
import com.minewright.llm.CompanionPromptBuilder;
import com.minewright.memory.ConversationManager;
import com.minewright.entity.ForemanEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * Conversationalist Specialist - handles player dialogue.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Maintains conversation with player</li>
 *   <li>Tells jokes and references shared experiences</li>
 *   <li>Responds to player questions</li>
 *   <li>Builds rapport through natural dialogue</li>
 * </ul>
 */
public class ConversationalistSpecialist implements KnowledgeSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationalSpecialist.class);

    private static final String ID = "conversationalist";
    private static final int PRIORITY = 70;

    private Blackboard blackboard;
    private final ForemanEntity foremanEntity;
    private final ConversationManager conversationManager;
    private final Random random;

    private Instant lastResponseTime;
    private int responseCooldown = 50;  // Ticks between responses

    public ConversationalistSpecialist(ForemanEntity foremanEntity) {
        this.foremanEntity = foremanEntity;
        this.conversationManager = new ConversationManager(foremanEntity);
        this.random = new Random();
        this.lastResponseTime = Instant.now().minusSeconds(10);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void setBlackboard(Blackboard blackboard) {
        this.blackboard = blackboard;
    }

    @Override
    public boolean canContribute() {
        // Can contribute if:
        // 1. There's a recent message from player
        // 2. Not in cooldown
        // 3. Not currently planning tasks
        List<ConversationMessage> history = blackboard.getConversationHistory(5);

        boolean hasPlayerMessage = history.stream()
            .anyMatch(m -> "player".equals(m.sender()) &&
                ChronoUnit.SECONDS.between(m.timestamp(), Instant.now()) < 30);

        boolean inCooldown = ChronoUnit.SECONDS.between(lastResponseTime, Instant.now()) < responseCooldown / 20;

        boolean isPlanningTopic = blackboard.getCurrentTopic() != null
            && blackboard.getCurrentTopic().startsWith("cmd:");

        return hasPlayerMessage && !inCooldown && !isPlanningTopic;
    }

    @Override
    public void evaluate() {
        List<ConversationMessage> history = blackboard.getConversationHistory(10);

        // Find the most recent player message
        ConversationMessage playerMessage = history.stream()
            .filter(m -> "player".equals(m.sender()))
            .findFirst()
            .orElse(null);

        if (playerMessage == null) {
            return;
        }

        String playerInput = playerMessage.content();
        LOGGER.debug("[Conversationalist] Processing: {}", playerInput);

        // Check if this is a task command or casual chat
        if (CompanionPromptBuilder.isTaskCommand(playerInput)) {
            // Set topic for planner to process
            blackboard.setCurrentTopic("cmd:" + playerInput);
            LOGGER.info("[Conversationalist] Recognized task command, delegating to Planner");
            return;
        }

        // This is casual chat - generate conversation
        generateConversation(playerInput);
    }

    private void generateConversation(String playerInput) {
        String systemPrompt = CompanionPromptBuilder.buildConversationalSystemPromptWithMemories(
            blackboard.getCompanionMemory(),
            playerInput
        );

        String userPrompt = CompanionPromptBuilder.buildConversationalUserPrompt(
            playerInput,
            blackboard.getCompanionMemory(),
            foremanEntity
        );

        conversationManager.generateConversation(systemPrompt, userPrompt)
            .thenAccept(response -> {
                if (response != null && !response.isEmpty()) {
                    // Add response to conversation
                    blackboard.addConversationMessage(new ConversationMessage(
                        Instant.now(),
                        "foreman",
                        "player",
                        response,
                        "chat",
                        extractTopic(playerInput)
                    ));

                    lastResponseTime = Instant.now();

                    LOGGER.debug("[Conversationalist] Responded: {}", response);
                }
            })
            .exceptionally(error -> {
                LOGGER.error("[Conversationalist] Error generating conversation", error);
                return null;
            });
    }

    private String extractTopic(String message) {
        // Simple topic extraction
        String[] words = message.toLowerCase().split("\\s+");
        return words.length > 0 ? words[0] : "general";
    }

    @Override
    public double getConfidence() {
        // Confidence based on rapport level and conversation history
        int rapport = blackboard.getCompanionMemory().getRapportLevel();
        return Math.min(1.0, rapport / 100.0 + 0.3);
    }

    @Override
    public void reset() {
        lastResponseTime = Instant.now().minusSeconds(10);
    }
}
```

### 3.4 Coordinator Specialist

```java
package com.minewright.blackboard.specialists;

import com.minewright.MineWrightMod;
import com.minewright.blackboard.*;
import com.minewright.action.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Coordinator Specialist - assigns tasks to workers.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Assigns pending tasks to available workers</li>
 *   <li>Monitors worker status and progress</li>
 *   <li>Handles failed tasks and retries</li>
 *   <li>Rebalances workload when workers complete</li>
 * </ul>
 */
public class CoordinatorSpecialist implements KnowledgeSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinatorSpecialist.class);

    private static final String ID = "coordinator";
    private static final int PRIORITY = 60;

    private static final int MAX_RETRIES = 2;
    private static final long WORKER_TIMEOUT_SECONDS = 60;

    private Blackboard blackboard;
    private final Map<String, Integer> taskRetryCount;
    private Instant lastCheckTime;

    public CoordinatorSpecialist() {
        this.taskRetryCount = new ConcurrentHashMap<>();
        this.lastCheckTime = Instant.now();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void setBlackboard(Blackboard blackboard) {
        this.blackboard = blackboard;
    }

    @Override
    public boolean canContribute() {
        // Can contribute if:
        // 1. There are pending tasks
        // 2. There are available workers
        // 3. It's been at least 1 second since last check
        List<Task> pendingTasks = blackboard.getTasksByState(Task.State.PENDING);
        List<String> availableWorkers = blackboard.getAvailableWorkers();

        boolean hasWork = !pendingTasks.isEmpty() && !availableWorkers.isEmpty();

        long secondsSinceCheck = ChronoUnit.SECONDS.between(lastCheckTime, Instant.now());

        return hasWork && secondsSinceCheck >= 1;
    }

    @Override
    public void evaluate() {
        lastCheckTime = Instant.now();

        List<Task> pendingTasks = blackboard.getTasksByState(Task.State.PENDING);
        List<String> availableWorkers = blackboard.getAvailableWorkers();

        if (pendingTasks.isEmpty() || availableWorkers.isEmpty()) {
            return;
        }

        LOGGER.info("[Coordinator] Assigning {} tasks to {} workers",
            pendingTasks.size(), availableWorkers.size());

        // Simple round-robin assignment
        Iterator<String> workerIterator = availableWorkers.iterator();
        int assignedCount = 0;

        for (Task task : pendingTasks) {
            if (!workerIterator.hasNext()) {
                workerIterator = availableWorkers.iterator();  // Cycle back
            }

            String workerId = workerIterator.next();
            assignTaskToWorker(task, workerId);
            assignedCount++;
        }

        LOGGER.info("[Coordinator] Assigned {} tasks", assignedCount);
    }

    private void assignTaskToWorker(Task task, String workerId) {
        // Find the task ID
        String taskId = blackboard.getAllTasks().stream()
            .filter(t -> t == task)
            .findFirst()
            .map(t -> "task_" + System.identityHashCode(t))
            .orElse("unknown");

        blackboard.assignTask(taskId, workerId);

        LOGGER.info("[Coordinator] Assigned task {} to worker {}",
            task.getAction(), workerId);

        // Record the assignment
        blackboard.recordExperience(
            "task_assignment",
            "Assigned " + task.getAction() + " to " + workerId,
            1
        );
    }

    /**
     * Called periodically to check for stuck/failed tasks.
     */
    public void checkWorkerHealth() {
        Map<String, WorkerInfo> workers = blackboard.getAllWorkers();
        Map<String, String> assignments = blackboard.getSnapshot().taskAssignments();

        for (Map.Entry<String, String> entry : assignments.entrySet()) {
            String taskId = entry.getKey();
            String workerId = entry.getValue();

            // Check if worker still exists and is responsive
            if (!workers.containsKey(workerId)) {
                LOGGER.warn("[Coordinator] Worker {} disappeared, reassigning task {}",
                    workerId, taskId);
                handleWorkerDisappeared(taskId, workerId);
            }
        }
    }

    private void handleWorkerDisappeared(String taskId, String workerId) {
        // Mark task as pending for reassignment
        // (This would need task state modification support)
        taskRetryCount.put(taskId, taskRetryCount.getOrDefault(taskId, 0) + 1);

        if (taskRetryCount.get(taskId) >= MAX_RETRIES) {
            blackboard.failTask(taskId, "Worker unavailable after " + MAX_RETRIES + " attempts");
            taskRetryCount.remove(taskId);
        }
    }

    @Override
    public double getConfidence() {
        // Confidence based on worker availability
        List<String> availableWorkers = blackboard.getAvailableWorkers();
        return Math.min(1.0, availableWorkers.size() / 5.0);
    }

    @Override
    public void reset() {
        taskRetryCount.clear();
        lastCheckTime = Instant.now();
    }
}
```

### 3.5 Memory Keeper Specialist

```java
package com.minewright.blackboard.specialists;

import com.minewright.MineWrightMod;
import com.minewright.blackboard.Blackboard;
import com.minewright.blackboard.ConversationMessage;
import com.minewright.blackboard.KnowledgeSource;
import com.minewright.memory.CompanionMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Memory Keeper Specialist - manages memories and learning.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Extracts facts from conversations</li>
 *   <li>Records experiences worth remembering</li>
 *   <li>Identifies inside jokes and memorable moments</li>
 *   <li>Updates player preferences</li>
 * </ul>
 */
public class MemoryKeeperSpecialist implements KnowledgeSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryKeeperSpecialist.class);

    private static final String ID = "memory_keeper";
    private static final int PRIORITY = 50;

    private Blackboard blackboard;
    private Instant lastUpdate;
    private int processedMessageCount = 0;

    public MemoryKeeperSpecialist() {
        this.lastUpdate = Instant.now();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void setBlackboard(Blackboard blackboard) {
        this.blackboard = blackboard;
    }

    @Override
    public boolean canContribute() {
        // Can contribute if there are new unprocessed messages
        List<ConversationMessage> history = blackboard.getConversationHistory(20);
        return history.size() > processedMessageCount;
    }

    @Override
    public void evaluate() {
        List<ConversationMessage> history = blackboard.getConversationHistory(20);

        // Process new messages
        int newMessages = history.size() - processedMessageCount;

        if (newMessages <= 0) {
            return;
        }

        LOGGER.debug("[MemoryKeeper] Processing {} new messages", newMessages);

        for (int i = history.size() - newMessages; i < history.size(); i++) {
            ConversationMessage message = history.get(i);
            processMessage(message);
        }

        processedMessageCount = history.size();
        lastUpdate = Instant.now();
    }

    private void processMessage(ConversationMessage message) {
        CompanionMemory memory = blackboard.getCompanionMemory();

        // Extract facts from player messages
        if ("player".equals(message.sender())) {
            extractFacts(message.content(), memory);
        }

        // Check for inside jokes (laughter, humor indicators)
        if (message.content().toLowerCase().contains("haha")
            || message.content().toLowerCase().contains("lol")
            || message.content().toLowerCase().contains("😄")) {
            String context = blackboard.getConversationHistory(3).stream()
                .map(ConversationMessage::content)
                .reduce((a, b) -> a + " " + b)
                .orElse("");

            if (context.length() > 0) {
                memory.recordInsideJoke(context, message.content());
                LOGGER.debug("[MemoryKeeper] Recorded inside joke");
            }
        }

        // Update rapport based on positive sentiment
        if (hasPositiveSentiment(message.content())) {
            memory.adjustRapport(1);
        }
    }

    private void extractFacts(String message, CompanionMemory memory) {
        String lower = message.toLowerCase();

        // Extract preferences
        if (lower.contains("i like") || lower.contains("i love") {
            String preference = extractPreference(message);
            if (preference != null) {
                memory.learnPlayerFact("preference", "likes", preference);
            }
        }

        if (lower.contains("i hate") || lower.contains("i dislike")) {
            String preference = extractPreference(message);
            if (preference != null) {
                memory.learnPlayerFact("preference", "dislikes", preference);
            }
        }

        // Extract skill mentions
        if (lower.contains("i'm good at") || lower.contains("i am good at")) {
            String skill = message.substring(lower.indexOf("good at") + 7).trim();
            memory.learnPlayerFact("skill", "good_at", skill);
        }
    }

    private String extractPreference(String message) {
        // Simple extraction - would be better with NLP
        String[] indicators = {"i like", "i love", "i hate", "i dislike"};
        for (String indicator : indicators) {
            int index = message.toLowerCase().indexOf(indicator);
            if (index >= 0) {
                return message.substring(index + indicator.length()).trim();
            }
        }
        return null;
    }

    private boolean hasPositiveSentiment(String message) {
        String lower = message.toLowerCase();
        return lower.contains("thank") || lower.contains("good job")
            || lower.contains("well done") || lower.contains("great")
            || lower.contains("awesome") || lower.contains("nice");
    }

    @Override
    public double getConfidence() {
        // Confidence based on amount of data collected
        int interactionCount = blackboard.getCompanionMemory().getInteractionCount();
        return Math.min(1.0, interactionCount / 100.0);
    }

    @Override
    public void reset() {
        processedMessageCount = 0;
        lastUpdate = Instant.now();
    }
}
```

---

## 4. Controller Logic

### 4.1 Blackboard Controller

```java
package com.minewright.blackboard;

import com.minewright.MineWrightMod;
import com.minewright.entity.ForemanEntity;
import com.minewright.blackboard.specialists.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for the blackboard system.
 *
 * <p>Responsible for:</p>
 * <ul>
 *   <li>Initializing the blackboard and knowledge sources</li>
 *   <li>Triggering scans when appropriate</li>
 *   <li>Managing the evaluation cycle</li>
 *   <li>Handling external events</li>
 * </ul>
 */
public class BlackboardController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlackboardController.class);

    private final ForemanBlackboard blackboard;
    private final ForemanEntity foremanEntity;

    // Knowledge sources
    private final PlannerSpecialist planner;
    private final ConversationalistSpecialist conversationalist;
    private final CoordinatorSpecialist coordinator;
    private final MemoryKeeperSpecialist memoryKeeper;

    // Periodic scan scheduler
    private final ScheduledExecutorService scheduler;

    public BlackboardController(ForemanEntity foremanEntity) {
        this.foremanEntity = foremanEntity;
        this.blackboard = new ForemanBlackboard(foremanEntity.getCompanionMemory());

        // Create specialists
        this.planner = new PlannerSpecialist(foremanEntity);
        this.conversationalist = new ConversationalistSpecialist(foremanEntity);
        this.coordinator = new CoordinatorSpecialist();
        this.memoryKeeper = new MemoryKeeperSpecialist();

        // Register specialists with blackboard
        blackboard.registerSource(planner);
        blackboard.registerSource(conversationalist);
        blackboard.registerSource(coordinator);
        blackboard.registerSource(memoryKeeper);

        // Setup periodic scanning
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "Blackboard-Controller");
            thread.setDaemon(true);
            return thread;
        });

        // Scan every 5 ticks (250ms)
        this.scheduler.scheduleAtFixedRate(
            this::periodicScan,
            0,
            250,
            TimeUnit.MILLISECONDS
        );

        LOGGER.info("BlackboardController initialized");
    }

    /**
     * Called every tick from the main game loop.
     */
    public void tick() {
        // Check for worker health periodically
        if (foremanEntity.tickCount % 100 == 0) {
            coordinator.checkWorkerHealth();
        }

        // Auto-trigger scan when significant changes occur
        if (shouldTriggerScan()) {
            blackboard.triggerScan();
        }
    }

    /**
     * Periodic scan called by scheduler.
     */
    private void periodicScan() {
        try {
            // Check if any source can contribute
            if (hasReadyContributors()) {
                blackboard.triggerScan();
            }
        } catch (Exception e) {
            LOGGER.error("Error in periodic scan", e);
        }
    }

    private boolean shouldTriggerScan() {
        // Trigger scan if:
        // - New conversation message arrived
        // - Task state changed
        // - Worker became available

        List<ConversationMessage> history = blackboard.getConversationHistory(1);
        if (!history.isEmpty()) {
            long secondsAgo = java.time.temporal.ChronoUnit.SECONDS.between(
                history.get(0).timestamp(),
                java.time.Instant.now()
            );
            if (secondsAgo < 1) {
                return true;  // Recent message
            }
        }

        // Check for pending tasks
        if (!blackboard.getTasksByState(com.minewright.action.Task.State.PENDING).isEmpty()) {
            if (!blackboard.getAvailableWorkers().isEmpty()) {
                return true;  // Work to assign
            }
        }

        return false;
    }

    private boolean hasReadyContributors() {
        return planner.canContribute()
            || conversationalist.canContribute()
            || coordinator.canContribute()
            || memoryKeeper.canContribute();
    }

    /**
     * Handles a player command.
     */
    public void handlePlayerCommand(String command) {
        LOGGER.info("Player command: {}", command);

        // Add to conversation
        blackboard.addConversationMessage(new ConversationMessage(
            java.time.Instant.now(),
            "player",
            "foreman",
            command,
            "command",
            null
        ));

        // Trigger immediate scan
        blackboard.triggerScan();
    }

    /**
     * Handles a worker status update.
     */
    public void handleWorkerStatus(String workerId, WorkerStatus status) {
        blackboard.updateWorkerStatus(workerId, status);

        // Trigger scan to reassign if needed
        if (status == WorkerStatus.AVAILABLE) {
            blackboard.triggerScan();
        }
    }

    /**
     * Gets the blackboard for external access.
     */
    public ForemanBlackboard getBlackboard() {
        return blackboard;
    }

    /**
     * Shuts down the controller.
     */
    public void shutdown() {
        scheduler.shutdown();
        blackboard.shutdown();

        LOGGER.info("BlackboardController shut down");
    }
}
```

### 4.2 Decision Logic Flow

```
┌──────────────────────────────────────────────────────────────┐
│                    Blackboard Controller                      │
└──────────────────────────────────────────────────────────────┘
         │
         │ 1. Check triggers
         ▼
┌──────────────────────────────────────────────────────────────┐
│ Should trigger scan?                                         │
│ - New conversation message?                                  │
│ - Task state changed?                                        │
│ - Worker became available?                                   │
└──────────────────────────────────────────────────────────────┘
         │
         │ Yes
         ▼
┌──────────────────────────────────────────────────────────────┐
│ Evaluate all knowledge sources (by priority)                 │
└──────────────────────────────────────────────────────────────┘
         │
         ├─────────────────────────────────────────────────┐
         │                                                 │
         ▼                                                 ▼
┌─────────────────────┐                           ┌─────────────────────┐
│ Planner             │                           │ Conversationalist  │
│ Priority: 90        │                           │ Priority: 70        │
│                     │                           │                     │
│ Can contribute?     │                           │ Can contribute?     │
│ - Not planning      │                           │ - Player message?   │
│ - Command pending   │                           │ - Not on cooldown   │
│ - Capacity < 50     │                           │ - Not planning      │
└─────────────────────┘                           └─────────────────────┘
         │                                                 │
         │ Yes                                             │ Yes
         ▼                                                 ▼
┌─────────────────────┐                           ┌─────────────────────┐
│ Evaluate:           │                           │ Evaluate:           │
│ 1. Get command      │                           │ 1. Check if task    │
│ 2. Call LLM         │                           │    command          │
│ 3. Validate tasks   │                           │ 2. If task: set     │
│ 4. Add to blackboard│                           │    topic for planner│
│ 5. Trigger scan     │                           │ 3. If chat: generate│
└─────────────────────┘                           │    response         │
                                                  └─────────────────────┘
         │
         ├─────────────────────────────────────────────────┐
         │                                                 │
         ▼                                                 ▼
┌─────────────────────┐                           ┌─────────────────────┐
│ Coordinator         │                           │ Memory Keeper       │
│ Priority: 60        │                           │ Priority: 50        │
│                     │                           │                     │
│ Can contribute?     │                           │ Can contribute?     │
│ - Pending tasks?    │                           │ - New messages?     │
│ - Workers avail?    │                           │ - Unprocessed?      │
└─────────────────────┘                           └─────────────────────┘
         │                                                 │
         │ Yes                                             │ Yes
         ▼                                                 ▼
┌─────────────────────┐                           ┌─────────────────────┐
│ Evaluate:           │                           │ Evaluate:           │
│ 1. Get pending tasks│                           │ 1. Extract facts    │
│ 2. Get avail workers│                           │ 2. Check for jokes  │
│ 3. Round-robin assign│                           │ 3. Update rapport   │
│ 4. Update state     │                           │ 4. Record memories  │
└─────────────────────┘                           └─────────────────────┘
```

---

## 5. Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            FOREMAN ENTITY                                │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   │ uses
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        BLACKBOARD CONTROLLER                             │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  - tick()                                 │  │
│  │  - handlePlayerCommand(command)                                    │  │
│  │  - handleWorkerStatus(workerId, status)                            │  │
│  │  - shouldTriggerScan(): boolean                                    │  │
│  │  - periodicScan()                                                  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   │ manages
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          FOREMAN BLACKBOARD                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐   │
│  │  Tasks   │  │ Workers  │  │Dialogue  │  │  Companion Memory    │   │
│  │          │  │          │  │History   │  │                      │   │
│  │ - Queue  │  │ - Status │  │ - Topic  │  │ - Rapport            │   │
│  │ - State  │  │ - Cap    │  │ - Msgs   │  │ - Trust              │   │
│  │ - Assign │  │          │  │          │  │ - Memories           │   │
│  └──────────┘  └──────────┘  └──────────┘  │ - Preferences        │   │
│                                                │ - Milestones         │   │
│  ┌─────────────────────────────────────────┤  └──────────────────────┘   │
│  │          World State                     │                              │
│  │  - Time of day                           │                              │
│  │  - Location                              │                              │
│  │  - Resources                             │                              │
│  │  - Weather                               │                              │
│  └─────────────────────────────────────────┘                              │
└─────────────────────────────────────────────────────────────────────────┘
           ▲               ▲               ▲               ▲
           │               │               │               │
           │ inspect       │ inspect       │ inspect       │ inspect
           │ contribute    │ contribute    │ contribute    │ contribute
           │               │               │               │
┌──────────┴──────┐ ┌─────┴──────┐ ┌──────┴──────┐ ┌─────┴──────────┐
│    PLANNER      │ │CONVERSATION│ │  MEMORY     │ │  COORDINATOR    │
│  Specialist     │ │ALIST       │ │  KEEPER     │ │  Specialist     │
│                 │ │            │ │             │ │                 │
│ Priority: 90    │ │Priority:70 │ │Priority:50  │ │Priority:60      │
│                 │ │            │ │             │ │                 │
│ canContribute() │ │canContrib()│ │canContrib() │ │canContribute()  │
│ evaluate()      │ │evaluate()  │ │evaluate()   │ │evaluate()       │
│                 │ │            │ │             │ │                 │
│ Uses:           │ │Uses:       │ │Uses:        │ │Uses:            │
│ - TaskPlanner   │ │ConvManager│ │Extract      │ │WorkerRegistry   │
│ - LLM           │ │LLM         │ │Facts        │ │TaskQueue        │
│ - Validator     │ │CompanionMem│ │Jokes        │ │                 │
└─────────────────┘ └────────────┘ └─────────────┘ └─────────────────┘
```

---

## 6. Data Flow Examples

### 6.1 Player Command Flow

```
Player: "Build a house"
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ BlackboardController.handlePlayerCommand("Build a house")        │
└──────────────────────────────────────────────────────────────────┘
         │
         │ 1. Add to conversation
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ Blackboard.addConversationMessage(                               │
│   sender: "player"                                                │
│   content: "Build a house"                                        │
│   type: "command"                                                 │
│ )                                                                 │
└──────────────────────────────────────────────────────────────────┘
         │
         │ 2. Trigger scan
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ Blackboard.triggerScan()                                         │
└──────────────────────────────────────────────────────────────────┘
         │
         │ 3. Evaluate specialists
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ Conversationalist.canContribute()                                │
│   -> Has player message? YES                                     │
│   -> Is task command? YES (CompanionPromptBuilder check)          │
│   -> Set currentTopic = "cmd:Build a house"                      │
└──────────────────────────────────────────────────────────────────┘
         │
         │ 4. Next scan cycle
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ Planner.canContribute()                                          │
│   -> Not planning? YES                                           │
│   -> Topic starts with "cmd:"? YES                               │
│   -> Capacity < 50? YES                                          │
│                                                                  │
│ Planner.evaluate()                                               │
│   -> taskPlanner.planTasksAsync("Build a house")                 │
│   -> (async) LLM returns tasks:                                  │
│      [                                                             │
│        {action: "pathfind", x: 100, y: 64, z: 100}               │
│        {action: "mine", block: "cobblestone", quantity: 500}      │
│        {action: "build", structure: "house"}                      │
│      ]                                                             │
│   -> blackboard.addTask(...) for each task                       │
│   -> clear currentTopic                                           │
└──────────────────────────────────────────────────────────────────┘
         │
         │ 5. Next scan cycle
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ Coordinator.canContribute()                                      │
│   -> Pending tasks? YES (3 tasks)                                │
│   -> Available workers? YES                                      │
│                                                                  │
│ Coordinator.evaluate()                                           │
│   -> Assign pathfind to Worker1                                  │
│   -> Assign mine to Worker2                                      │
│   -> Assign build to Worker1 (when pathfind complete)            │
└──────────────────────────────────────────────────────────────────┘
```

### 6.2 Casual Conversation Flow

```
Player: "How are you doing?"
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ BlackboardController.handlePlayerCommand("How are you doing?")   │
└──────────────────────────────────────────────────────────────────┘
         │
         │ Add to conversation + Trigger scan
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ Conversationalist.canContribute()                                │
│   -> Has player message? YES                                     │
│   -> Is task command? NO (CompanionPromptBuilder check)           │
│   -> On cooldown? NO                                             │
│                                                                  │
│ Conversationalist.evaluate()                                     │
│   -> generateConversation("How are you doing?")                  │
│   -> LLM generates: "Pretty good! We've built 47 structures     │
│      together. Ready for more work!"                              │
│   -> Add response to conversation                                │
└──────────────────────────────────────────────────────────────────┘
         │
         │ Next scan cycle
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ MemoryKeeper.canContribute()                                     │
│   -> New unprocessed messages? YES                               │
│                                                                  │
│ MemoryKeeper.evaluate()                                          │
│   -> Extract facts from conversation                             │
│   -> Check for humor/jokes                                       │
│   -> Update rapport (positive sentiment detected)                │
└──────────────────────────────────────────────────────────────────┘
```

---

## 7. Pros and Cons

### 7.1 Advantages

#### 7.1.1 Flexibility
- **Multiple AI Approaches:** Different specialists can use different AI techniques (LLM, rules, ML) without interfering
- **Easy Extension:** Add new specialists by implementing the `KnowledgeSource` interface
- **Independent Development:** Each specialist can be developed and tested independently

#### 7.1.2 Emergent Behavior
- **Collaborative Intelligence:** Complex problem-solving emerges from specialist interactions
- **Adaptive Responses:** System adapts to situations based on which specialists activate
- **Rich Interactions:** Conversation, task planning, and memory can interleave naturally

#### 7.1.3 Maintainability
- **Clear Separation:** Each specialist has a single, well-defined responsibility
- **Isolated Changes:** Modifying one specialist doesn't affect others
- **Observable State:** Blackboard provides complete visibility into system state

#### 7.1.4 Resilience
- **Fault Isolation:** Failure in one specialist doesn't crash the system
- **Graceful Degradation:** System continues with remaining specialists if one fails
- **Retry Logic:** Tasks can be reassigned if a worker/specialist fails

### 7.2 Disadvantages

#### 7.2.1 Coordination Complexity
- **Conflict Resolution:** Multiple specialists may try to act on the same data
- **Priority Management:** Need careful design of specialist priorities
- **Race Conditions:** Concurrent access to shared blackboard requires synchronization

#### 7.2.2 Predictability Challenges
- **Emergent Behavior:** Can be difficult to predict exactly how the system will respond
- **Debugging Difficulty:** Issues may arise from complex specialist interactions
- **Testing Complexity:** Need to test many specialist combination scenarios

#### 7.2.3 Performance Overhead
- **Scan Cycles:** Regular scans consume CPU even when idle
- **Synchronization:** Thread-safe operations add overhead
- **Memory Usage:** Maintaining full state in memory can be expensive

#### 7.2.4 Design Challenges
- **Specialist Design:** Deciding how to split responsibilities is non-trivial
- **Trigger Conditions:** Determining when specialists should activate requires careful tuning
- **Blackboard Schema:** Designing a schema that serves all specialists can be difficult

### 7.3 Comparison to Current Implementation

| Aspect | Current (OrchestratorService) | Blackboard Architecture |
|--------|-------------------------------|-------------------------|
| **Coordination** | Centralized in OrchestratorService | Distributed across specialists |
| **Communication** | Direct method calls + AgentMessage | Shared blackboard state |
| **Extensibility** | Add methods to OrchestratorService | Implement KnowledgeSource interface |
| **Task Assignment** | Explicit distribution in OrchestratorService | Opportunistic by Coordinator specialist |
| **Conversation** | Handled separately | Integrated as Conversationalist specialist |
| **Memory** | CompanionMemory separate | Integrated via MemoryKeeper specialist |
| **Complexity** | Moderate (single orchestrator) | Higher (multiple specialists) |
| **Flexibility** | Limited (orchestrator defines flow) | High (any specialist can drive) |

---

## 8. Complexity Rating: 7/10

### 8.1 Complexity Breakdown

| Component | Complexity | Justification |
|-----------|-----------|---------------|
| **Blackboard Core** | 6/10 | Thread-safe shared state, event listeners, snapshot management |
| **Knowledge Sources** | 7/10 | Each source needs independent logic, trigger conditions, state management |
| **Controller** | 5/10 | Straightforward scan triggering, simple decision logic |
| **Integration** | 8/10 | Integrating with existing OrchestratorService, ActionExecutor requires care |
| **Testing** | 9/10 | Testing emergent behavior and specialist interactions is complex |

### 8.2 Development Effort Estimate

| Phase | Effort | Description |
|-------|--------|-------------|
| **Design** | 2-3 days | Design blackboard schema, specialist interfaces, integration points |
| **Blackboard Core** | 3-4 days | Implement thread-safe blackboard with all data structures |
| **Specialists** | 5-7 days | Implement 4 core specialists (Planner, Conversationalist, Coordinator, Memory) |
| **Controller** | 2-3 days | Implement controller with scan scheduling |
| **Integration** | 3-4 days | Integrate with MineWrightEntity, ActionExecutor, OrchestratorService |
| **Testing** | 4-5 days | Unit tests, integration tests, emergent behavior scenarios |
| **Polish** | 2-3 days | Performance optimization, debugging tools, logging |
| **Total** | **21-29 days** | Approximately 4-6 weeks for one developer |

### 8.3 Risk Factors

1. **Integration Risk (Medium):** Existing codebase uses OrchestratorService pattern; migration to blackboard may require refactoring
2. **Performance Risk (Low):** Scan cycles and synchronization overhead is manageable
3. **Complexity Risk (Medium-High):** Emergent behavior can be unpredictable; thorough testing required
4. **Maintainability Risk (Low):** Once established, adding specialists is straightforward

---

## 9. Implementation Roadmap

### Phase 1: Foundation (Week 1)
- [ ] Create `Blackboard` interface and `ForemanBlackboard` implementation
- [ ] Create `KnowledgeSource` interface
- [ ] Implement basic `BlackboardController`
- [ ] Add unit tests for blackboard core

### Phase 2: Core Specialists (Week 2-3)
- [ ] Implement `PlannerSpecialist` with LLM integration
- [ ] Implement `ConversationalistSpecialist` with dialogue
- [ ] Implement `CoordinatorSpecialist` with worker assignment
- [ ] Add integration tests

### Phase 3: Memory and Learning (Week 4)
- [ ] Implement `MemoryKeeperSpecialist`
- [ ] Add fact extraction from conversations
- [ ] Add inside joke detection
- [ ] Test memory-based responses

### Phase 4: Integration (Week 5)
- [ ] Integrate with `MineWrightEntity`
- [ ] Connect with `ActionExecutor`
- [ ] Migrate `OrchestratorService` to use blackboard (or keep hybrid)
- [ ] End-to-end testing

### Phase 5: Polish and Optimization (Week 6)
- [ ] Performance profiling and optimization
- [ ] Add debugging tools (blackboard inspector)
- [ ] Comprehensive testing scenarios
- [ ] Documentation

---

## 10. Conclusion

The Blackboard Architecture offers a powerful pattern for the MineWright AI foreman system, enabling multiple AI specialists to collaborate through a shared knowledge base. While more complex than the current centralized orchestration approach, it provides significant benefits in flexibility, extensibility, and emergent behavior.

**Key Takeaways:**

1. **Best For:** Complex, multi-faceted AI problems where different expertise domains need to collaborate
2. **Implementation:** Requires careful design of blackboard schema and specialist triggers
3. **Integration:** Can coexist with current OrchestratorService as a migration path
4. **Value:** Enables rich interactions where conversation, task planning, and memory seamlessly integrate

**Recommendation:** Implement as a gradual migration, starting with the blackboard core and one specialist (e.g., Conversationalist), then adding others incrementally while maintaining the existing OrchestratorService as a fallback.

---

## Appendix A: Supporting Classes

```java
// Task.State extension
package com.minewright.action {
    public enum State {
        PENDING,
        ASSIGNED,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}

// WorkerCapabilities
package com.minewright.blackboard {
    public record WorkerCapabilities(
        Set<String> capabilities,  // "mine", "build", "pathfind", etc.
        int maxTasks,
        double speedMultiplier
    ) {
        public boolean hasCapability(String capability) {
            return capabilities.contains(capability);
        }
    }
}

// WorkerStatus
package com.minewright.blackboard {
    public enum WorkerStatus {
        AVAILABLE,
        BUSY,
        STUCK,
        DISCONNECTED
    }
}

// WorkerInfo
package com.minewright.blackboard {
    public record WorkerInfo(
        String id,
        String name,
        WorkerCapabilities capabilities
    ) {}
}

// TaskProgress
package com.minewright.blackboard {
    public record TaskProgress(
        int percentComplete,
        String status,
        Instant lastUpdate
    ) {}
}

// ConversationMessage
package com.minewright.blackboard {
    public record ConversationMessage(
        Instant timestamp,
        String sender,      // "player", "foreman", "worker_X"
        String recipient,   // "player", "foreman", "worker_X", or "*"
        String content,
        String type,        // "command", "chat", "system", "error"
        String topic        // Optional topic tag
    ) {}
}

// WorldState
package com.minewright.blackboard {
    public record WorldState(
        long gameTime,
        String biome,
        String weather,
        Map<String, Object> facts
    ) {
        public WorldState() {
            this(0, "plains", "clear", new HashMap<>());
        }
    }
}

// BlackboardEvent
package com.minewright.blackboard {
    public sealed interface BlackboardEvent {
        record TaskAdded(String taskId, Task task) implements BlackboardEvent {}
        record TaskAssigned(String taskId, String workerId) implements BlackboardEvent {}
        record TaskCompleted(String taskId, String result) implements BlackboardEvent {}
        record TaskFailed(String taskId, String reason) implements BlackboardEvent {}
        record WorkerRegistered(String workerId, String name) implements BlackboardEvent {}
        record ConversationAdded(ConversationMessage message) implements BlackboardEvent {}
        record WorldStateChanged(WorldState state) implements BlackboardEvent {}
        // ... more event types
    }
}

// BlackboardListener
package com.minewright.blackboard {
    @FunctionalInterface
    public interface BlackboardListener {
        void onBlackboardEvent(BlackboardEvent event);
    }
}

// BlackboardSnapshot
package com.minewright.blackboard {
    public record BlackboardSnapshot(
        Map<String, Task> tasks,
        Map<String, Task.State> taskStates,
        Map<String, WorkerInfo> workers,
        List<ConversationMessage> conversationHistory,
        WorldState worldState,
        Map<String, WorkerStatus> workerStatuses
    ) {}
}

// Contribution
package com.minewright.blackboard {
    public record Contribution(
        long id,
        String sourceId,
        String type,
        String description,
        Instant timestamp
    ) {}
}
```
