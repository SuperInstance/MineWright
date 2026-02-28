# Event-Driven Architecture - MineWright Foreman System

**Author:** Architecture Agent
**Date:** 2026-02-26
**Version:** 1.0
**Complexity Rating:** 6/10

---

## 1. Overview

The MineWright system implements an event-driven architecture (EDA) that enables loose coupling between components through asynchronous event publication and subscription. This architecture is fundamental to the multi-agent orchestration system, allowing the Foreman to coordinate crew members without direct dependencies.

### Key Principles

1. **Decoupling**: Publishers don't know about subscribers
2. **Asynchronous Communication**: Non-blocking event delivery
3. **Observability**: All state changes are observable via events
4. **Extensibility**: New behaviors can be added by subscribing to events
5. **Error Isolation**: Subscriber failures don't affect other subscribers

### Event-Driven Flow in the Foreman System

```
Human Player Command
         |
         v
    [Foreman MineWright]
         |
         |-----> EventBus.publish(StateTransitionEvent)
         |      |---> LoggingInterceptor (log state change)
         |      |---> MetricsInterceptor (track metrics)
         |      |---> OrchestratorService (react to state)
         |
         |-----> EventBus.publish(ActionStartedEvent)
         |      |---> EventPublishingInterceptor (track action)
         |      |---> GUI (update display)
         |      |---> Metrics (record start time)
         |
         v
    [Task Distribution]
         |
         |-----> AgentCommunicationBus.publish(AgentMessage)
         |      |---> Worker MineWright 1 (receive task)
         |      |---> Worker MineWright 2 (receive task)
         |      |---> Worker MineWright 3 (receive task)
         |
         v
    [Worker Execution]
         |
         |-----> EventBus.publish(ActionCompletedEvent)
         |      |---> Foreman (update progress)
         |      |---> OrchestratorService (check plan completion)
         |      |---> GUI (show progress bar)
```

---

## 2. Event Types

The MineWright AI system uses multiple event hierarchies for different purposes:

### 2.1 Action Lifecycle Events

Events published during action execution lifecycle.

#### ActionStartedEvent
```java
public class ActionStartedEvent {
    private final String agentId;
    private final String actionName;
    private final String description;
    private final Map<String, Object> parameters;
    private final Instant timestamp;
}
```
- **Publisher**: EventPublishingInterceptor
- **Subscribers**: Logging system, metrics collectors, GUI
- **Purpose**: Notify that an action has started execution
- **Trigger**: Before action execution begins

#### ActionCompletedEvent
```java
public class ActionCompletedEvent {
    private final String agentId;
    private final String actionName;
    private final boolean success;
    private final String message;
    private final long durationMs;
    private final Instant timestamp;
}
```
- **Publisher**: EventPublishingInterceptor
- **Subscribers**: OrchestratorService, metrics collectors, GUI
- **Purpose**: Notify that an action has completed (success or failure)
- **Trigger**: After action execution finishes

### 2.2 State Machine Events

Events published when agent state transitions occur.

#### StateTransitionEvent
```java
public class StateTransitionEvent {
    private final String agentId;
    private final AgentState fromState;
    private final AgentState toState;
    private final String reason;
    private final Instant timestamp;
}
```
- **Publisher**: AgentStateMachine
- **Subscribers**: Logging system, orchestrator, GUI
- **Purpose**: Track agent lifecycle state changes
- **Valid Transitions**:
  - IDLE → PLANNING
  - PLANNING → EXECUTING | FAILED
  - EXECUTING → COMPLETED | FAILED | PAUSED
  - PAUSED → EXECUTING | IDLE
  - COMPLETED → IDLE
  - FAILED → IDLE

### 2.3 Agent Communication Events

Messages passed between agents via the communication bus.

#### Task Assignment
```java
AgentMessage.Type.TASK_ASSIGNMENT
```
- **From**: Foreman
- **To**: Worker
- **Purpose**: Assign a task to a worker agent
- **Payload**: task description, parameters, task ID

#### Task Progress
```java
AgentMessage.Type.TASK_PROGRESS
```
- **From**: Worker
- **To**: Foreman
- **Purpose**: Report progress on assigned task
- **Payload**: percent complete, status message

#### Task Complete
```java
AgentMessage.Type.TASK_COMPLETE
AgentMessage.Type.TASK_FAILED
```
- **From**: Worker
- **To**: Foreman
- **Purpose**: Notify task completion or failure
- **Payload**: result, success flag

#### Status Messages
```java
AgentMessage.Type.STATUS_QUERY
AgentMessage.Type.STATUS_REPORT
```
- **Purpose**: Query and report agent status
- **Used by**: Foreman to check worker availability

#### Coordination Messages
```java
AgentMessage.Type.COORDINATION
AgentMessage.Type.HELP_REQUEST
```
- **Purpose**: Inter-worker coordination
- **Used for**: Workers helping each other, collaborative tasks

### 2.4 Game Events

Minecraft game events that trigger agent behaviors.

#### Player Action Events
- Player login/logout
- Block placement/breaking by player
- Chat commands

#### World Change Events
- Block changes (natural: tree growth, fluid flow)
- Entity spawning/despawning
- Chunk loading/unloading

#### Time Tick Events
- Game tick (20 ticks per second)
- Used for periodic status checks
- Progress reporting intervals

---

## 3. EventBus Design

The EventBus is the core component enabling event-driven communication.

### 3.1 Interface Definition

```java
public interface EventBus {
    /**
     * Subscribes to events of a specific type.
     */
    <T> Subscription subscribe(Class<T> eventType, Consumer<T> subscriber);

    /**
     * Subscribes with a priority (higher priority = called first).
     */
    <T> Subscription subscribe(Class<T> eventType, Consumer<T> subscriber, int priority);

    /**
     * Publishes an event to all subscribers (synchronous).
     */
    <T> void publish(T event);

    /**
     * Publishes an event asynchronously.
     */
    <T> void publishAsync(T event);

    /**
     * Unsubscribes all subscribers for an event type.
     */
    void unsubscribeAll(Class<?> eventType);

    /**
     * Returns the number of subscribers for an event type.
     */
    int getSubscriberCount(Class<?> eventType);
}
```

### 3.2 Implementation: SimpleEventBus

```java
public class SimpleEventBus implements EventBus {
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<SubscriberEntry<?>>> subscribers;
    private final ExecutorService asyncExecutor;

    @Override
    public <T> void publish(T event) {
        if (event == null) return;

        Class<?> eventType = event.getClass();
        CopyOnWriteArrayList<SubscriberEntry<?>> subs = subscribers.get(eventType);

        if (subs == null || subs.isEmpty()) {
            return;
        }

        // Notify all subscribers in priority order
        for (SubscriberEntry<?> entry : subs) {
            if (!entry.isActive()) continue;

            try {
                ((Consumer<T>) entry.subscriber).accept(event);
            } catch (Exception e) {
                LOGGER.error("Error in event subscriber for {}: {}",
                    eventType.getSimpleName(), e.getMessage(), e);
                // Continue to other subscribers - don't let one failure stop others
            }
        }
    }

    @Override
    public <T> void publishAsync(T event) {
        if (event == null) return;

        asyncExecutor.submit(() -> {
            try {
                publish(event);
            } catch (Exception e) {
                LOGGER.error("Error in async event publishing: {}", e.getMessage(), e);
            }
        });
    }
}
```

### 3.3 Thread Safety

- **ConcurrentHashMap**: Thread-safe subscriber registry
- **CopyOnWriteArrayList**: Thread-safe iteration during publish
- **AtomicBoolean**: Thread-safe subscription active flag
- **ExecutorService**: Dedicated thread for async publishing

### 3.4 Priority-Based Ordering

```java
@Override
public <T> Subscription subscribe(Class<T> eventType, Consumer<T> subscriber, int priority) {
    SubscriberEntry<T> entry = new SubscriberEntry<>(subscriber, priority);

    subscribers.compute(eventType, (key, list) -> {
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
        }
        list.add(entry);
        // Sort by priority (descending - higher priority first)
        list.sort((a, b) -> Integer.compare(b.priority, a.priority));
        return list;
    });

    return new SubscriptionImpl(eventType, entry);
}
```

**Priority Order**:
1. Critical interceptors (1000)
2. Event publishing (500)
3. Logging (100)
4. Metrics (50)
5. Debug listeners (0)

---

## 4. Foreman as Subscriber

The foreman (OrchestratorService) subscribes to various events to coordinate workers.

### 4.1 Subscribing to Worker Messages

```java
public void registerAgent(MineWrightEntity minewright, AgentRole role) {
    String agentId = minewright.getMineWrightName();

    // Register with communication bus
    communicationBus.registerAgent(agentId, agentName);

    // Subscribe to messages from this agent
    communicationBus.subscribe(agentId, message ->
        handleMessageFromAgent(agentId, message));
}
```

### 4.2 Handling Worker Events

```java
private void handleMessageFromAgent(String agentId, AgentMessage message) {
    switch (message.getType()) {
        case TASK_PROGRESS:
            handleTaskProgress(agentId, message);
            break;

        case TASK_COMPLETE:
            handleTaskComplete(agentId, message);
            break;

        case TASK_FAILED:
            handleTaskFailed(agentId, message);
            break;

        case HELP_REQUEST:
            handleHelpRequest(agentId, message);
            break;

        case STATUS_REPORT:
            handleStatusReport(agentId, message);
            break;
    }
}
```

### 4.3 Task Progress Handler

```java
private void handleTaskProgress(String workerId, AgentMessage message) {
    TaskAssignment assignment = workerAssignments.get(workerId);
    if (assignment != null) {
        int percent = message.getPayloadValue("percentComplete", 0);
        String status = message.getPayloadValue("status", "In progress");
        assignment.updateProgress(percent, status);

        LOGGER.debug("[Orchestrator] Task progress from {}: {}% - {}",
            workerId, percent, status);

        // Could trigger rebalancing if progress is too slow
    }
}
```

### 4.4 Task Completion Handler

```java
private void handleTaskComplete(String workerId, AgentMessage message) {
    TaskAssignment assignment = workerAssignments.remove(workerId);
    if (assignment != null) {
        String result = message.getPayloadValue("result", "Completed");
        assignment.complete(result);

        // Update plan
        PlanExecution plan = activePlans.get(assignment.getParentPlanId());
        if (plan != null) {
            plan.markTaskComplete(assignment.getAssignmentId());
            checkPlanCompletion(plan);
        }

        LOGGER.info("[Orchestrator] Task completed by {}: {}", workerId, result);

        // Notify human player
        notifyHumanPlayer(String.format("%s completed: %s", workerId, result));
    }
}
```

---

## 5. Event Flow Diagrams

### 5.1 Task Assignment Flow

```
┌─────────────┐
│ Human Player│
└──────┬──────┘
       │ "Build a house"
       ▼
┌─────────────────────┐
│   Foreman MineWright     │
│                     │
│ 1. StateTransition  │
│    IDLE → PLANNING  │
└──────┬──────────────┘
       │
       │ EventBus.publish(StateTransitionEvent)
       │
       ├──▶ LoggingInterceptor: "State changed"
       ├──▶ MetricsInterceptor: "Record planning time"
       └──▶ OrchestratorService: "Prepare for tasks"
       │
       ▼
┌─────────────────────┐
│   Task Planning     │
│   (LLM Call)        │
└──────┬──────────────┘
       │
       │ Returns 10 tasks
       ▼
┌─────────────────────┐
│ Task Distribution   │
└──────┬──────────────┘
       │
       │ AgentCommunicationBus.publish(TaskAssignment)
       │
       ├──▶ Worker 1: "Place blocks 1-3"
       ├──▶ Worker 2: "Place blocks 4-6"
       └──▶ Worker 3: "Place blocks 7-10"
       │
       ▼
┌─────────────────────┐
│  StateTransition    │
│  PLANNING → EXECUTING│
└─────────────────────┘
```

### 5.2 Worker Execution Flow

```
┌─────────────────────┐
│   Worker MineWright      │
│                     │
│ Receives: Task      │
│   Assignment        │
└──────┬──────────────┘
       │
       │ EventBus.publish(ActionStartedEvent)
       │
       ├──▶ EventPublishingInterceptor
       ├──▶ MetricsInterceptor
       └──▶ GUI (show "Working...")
       │
       ▼
┌─────────────────────┐
│  Action Execution   │
│  (ticking action)   │
└──────┬──────────────┘
       │
       │ Every 100 ticks:
       │ AgentCommunicationBus.publish(TaskProgress)
       │
       └──▶ Foreman: Update progress %
       │
       ▼
┌─────────────────────┐
│  Action Complete    │
└──────┬──────────────┘
       │
       │ EventBus.publish(ActionCompletedEvent)
       │
       ├──▶ EventPublishingInterceptor
       ├──▶ MetricsInterceptor (calculate duration)
       └──▶ Foreman (mark task complete)
       │
       ▼
┌─────────────────────┐
│ AgentCommunicationBus│
│ .publish(TaskComplete)│
└──────┬──────────────┘
       │
       └──▶ Foreman: Check if all tasks done
```

### 5.3 Failure Handling Flow

```
┌─────────────────────┐
│   Worker MineWright      │
│                     │
│ Action Fails        │
│ (e.g., can't find   │
│  path to target)    │
└──────┬──────────────┘
       │
       │ EventBus.publish(ActionCompletedEvent {success=false})
       │
       ▼
┌─────────────────────┐
│ AgentCommunicationBus│
│ .publish(TaskFailed) │
└──────┬──────────────┘
       │
       └──▶ Foreman OrchestratorService
            │
            │ Check retry count
            │
            ├── retryCount < MAX_RETRIES?
            │    │
            │    ├── YES: Reassign to different worker
            │    │       │
            │    │       └──▶ AgentCommunicationBus.publish(TaskAssignment)
            │    │                └──▶ Another Worker
            │    │
            │    └── NO: Mark task as failed
            │                 │
            │                 └──▶ PlanExecution.markTaskFailed()
            │                          │
            │                          └──▶ Notify human player
            │
            ▼
┌─────────────────────┐
│ Check Plan Status   │
│                     │
│ All tasks done?     │
│ (complete + failed) │
└─────────────────────┘
```

### 5.4 Event Bus Internal Flow

```
Publisher calls:        ┌─────────────────────────────────┐
eventBus.publish(event) │         SimpleEventBus            │
                        │                                 │
                        │  subscribers: ConcurrentHashMap│
└───────────────────────┤  ┌─────────────────────────────┐│
                       │  │ ActionCompletedEvent.class  ││
                       │  │  ┌───────────────────────┐  ││
                       │  │  │ SubscriberEntry[0]    │  ││
                       │  │  │ Priority: 1000        │  ││
                       │  │  │ Handler: Critical...  │  ││
                       │  │  └───────────────────────┘  ││
                       │  │  ┌───────────────────────┐  ││
                       │  │  │ SubscriberEntry[1]    │  ││
                       │  │  │ Priority: 500         │  ││
                       │  │  │ Handler: EventPub...  │  ││
                       │  │  └───────────────────────┘  ││
                       │  │  ┌───────────────────────┐  ││
                       │  │  │ SubscriberEntry[2]    │  ││
                       │  │  │ Priority: 100         │  ││
                       │  │  │ Handler: Logging...   │  ││
                       │  │  └───────────────────────┘  ││
                       │  └─────────────────────────────┘│
                        └─────────────────────────────────┘
                                    │
                                    │ Iterate in priority order
                                    ▼
                        ┌───────────────────────────────────┐
                        │  For each subscriber:             │
                        │  1. Check if active               │
                        │  2. Call subscriber.accept(event) │
                        │  3. Catch and log errors          │
                        │  4. Continue to next subscriber   │
                        └───────────────────────────────────┘
```

---

## 6. Code Examples

### 6.1 Subscribing to Events

```java
// Create event bus
EventBus eventBus = new SimpleEventBus();

// Subscribe to action started events
eventBus.subscribe(ActionStartedEvent.class, event -> {
    String agentId = event.getAgentId();
    String actionName = event.getActionName();
    LOGGER.info("[{}] Action started: {}", agentId, actionName);

    // Update GUI
    MineWrightGUI.addMineWrightMessage(agentId, "Starting: " + actionName);
}, 100); // Priority 100

// Subscribe to action completed events
eventBus.subscribe(ActionCompletedEvent.class, event -> {
    if (event.isSuccess()) {
        LOGGER.info("[{}] Action completed in {}ms",
            event.getAgentId(), event.getDurationMs());
    } else {
        LOGGER.warn("[{}] Action failed: {}",
            event.getAgentId(), event.getMessage());
    }
}, 100);

// Subscribe to state transitions
eventBus.subscribe(StateTransitionEvent.class, event -> {
    LOGGER.info("[{}] State: {} → {} ({})",
        event.getAgentId(),
        event.getFromState(),
        event.getToState(),
        event.getReason());
}, 50); // Lower priority
```

### 6.2 Publishing Events

```java
// Synchronous publish (blocking)
eventBus.publish(new ActionStartedEvent(
    "MineWright1",
    "mine",
    "Mining cobblestone",
    Map.of("quantity", 64, "block", "cobblestone")
));

// Asynchronous publish (non-blocking)
eventBus.publishAsync(new ActionCompletedEvent(
    "MineWright1",
    "mine",
    true,
    "Mined 64 cobblestone",
    5000 // duration in ms
));

// State transition
eventBus.publish(new StateTransitionEvent(
    "MineWright1",
    AgentState.IDLE,
    AgentState.PLANNING,
    "Received command from player"
));
```

### 6.3 Interceptor Chain with Events

```java
public class EventPublishingInterceptor implements ActionInterceptor {
    private final EventBus eventBus;
    private final String agentId;
    private final ConcurrentHashMap<Integer, Long> startTimes;

    @Override
    public boolean beforeAction(BaseAction action, ActionContext context) {
        // Record start time
        startTimes.put(System.identityHashCode(action), System.currentTimeMillis());

        // Publish ActionStartedEvent
        ActionStartedEvent event = new ActionStartedEvent(
            agentId,
            extractActionName(action),
            action.getDescription(),
            Map.of()
        );

        eventBus.publish(event);
        return true;
    }

    @Override
    public void afterAction(BaseAction action, ActionResult result, ActionContext context) {
        // Calculate duration
        Long startTime = startTimes.remove(System.identityHashCode(action));
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        // Publish ActionCompletedEvent
        ActionCompletedEvent event = new ActionCompletedEvent(
            agentId,
            extractActionName(action),
            result.isSuccess(),
            result.getMessage(),
            duration
        );

        eventBus.publish(event);
    }
}
```

### 6.4 Agent Communication via Message Bus

```java
// Foreman sends task assignment
AgentMessage taskMsg = AgentMessage.taskAssignment(
    "foreman",           // sender ID
    "Foreman",           // sender name
    "worker1",           // recipient ID
    "mine",              // task description
    Map.of(              // task parameters
        "block", "cobblestone",
        "quantity", 64
    )
);
communicationBus.publish(taskMsg);

// Worker receives task
AgentMessage message = communicationBus.poll("worker1");
if (message != null && message.getType() == AgentMessage.Type.TASK_ASSIGNMENT) {
    String task = message.getPayloadValue("taskDescription", "unknown");
    LOGGER.info("Received task: {}", task);

    // Execute task...

    // Send progress update
    AgentMessage progress = AgentMessage.taskProgress(
        "worker1", "Worker 1",
        "foreman",
        message.getMessageId(),
        50,  // 50% complete
        "Mining in progress"
    );
    communicationBus.publish(progress);

    // Send completion
    AgentMessage complete = AgentMessage.taskComplete(
        "worker1", "Worker 1",
        "foreman",
        message.getMessageId(),
        true,  // success
        "Mined 64 cobblestone"
    );
    communicationBus.publish(complete);
}
```

### 6.5 State Machine with Events

```java
public class AgentStateMachine {
    private final AtomicReference<AgentState> currentState;
    private final EventBus eventBus;
    private final String agentId;

    public boolean transitionTo(AgentState targetState, String reason) {
        AgentState fromState = currentState.get();

        // Check if transition is valid
        if (!canTransitionTo(targetState)) {
            LOGGER.warn("[{}] Invalid state transition: {} → {}",
                agentId, fromState, targetState);
            return false;
        }

        // Atomic compare-and-set
        if (currentState.compareAndSet(fromState, targetState)) {
            LOGGER.info("[{}] State transition: {} → {}",
                agentId, fromState, targetState);

            // Publish event to all subscribers
            if (eventBus != null) {
                eventBus.publish(new StateTransitionEvent(
                    agentId, fromState, targetState, reason
                ));
            }

            return true;
        }
        return false;
    }
}
```

---

## 7. Pros

### 7.1 Scalability
- **Horizontal Scaling**: Easy to add new subscribers without modifying publishers
- **Parallel Processing**: Multiple subscribers can process events concurrently
- **Load Distribution**: Workers can be added/removed dynamically

### 7.2 Loose Coupling
- **Publisher Independence**: Publishers don't know about subscribers
- **Easy Testing**: Subscribers can be tested in isolation
- **Flexible Architecture**: Components can be replaced without affecting others

### 7.3 Easy to Extend
- **New Behaviors**: Add new functionality by subscribing to events
- **Cross-Cutting Concerns**: Logging, metrics, security via interceptors
- **Plugin Architecture**: Actions can be added via registry

```java
// Example: Adding a new behavior without changing existing code
eventBus.subscribe(ActionCompletedEvent.class, event -> {
    // Send discord notification
    discordNotifier.sendMessage(
        event.getAgentId() + " completed " + event.getActionName()
    );
}, 10);
```

### 7.4 Observability
- **Complete Audit Trail**: All state changes are logged via events
- **Debugging**: Event history provides trace of what happened
- **Metrics**: Easy to collect metrics from events

### 7.5 Resilience
- **Error Isolation**: One subscriber's failure doesn't affect others
- **Retry Logic**: Failed tasks can be reassigned to different workers
- **Graceful Degradation**: System continues even if some subscribers fail

---

## 8. Cons

### 8.1 Debugging Complexity
- **Event Flow**: Harder to trace execution flow compared to direct calls
- **Async Challenges**: Events may be processed out of order
- **Hidden Dependencies**: Subscribers may have implicit dependencies

**Mitigation**:
- Use message history for debugging
- Add correlation IDs to track request flow
- Implement event ordering where needed

### 8.2 Event Ordering Issues
- **Race Conditions**: Multiple events may arrive out of order
- **State Synchronization**: Harder to keep state consistent
- **Priority Conflicts**: High-priority events may starve low-priority ones

**Mitigation**:
- Use sequence numbers for ordering
- Implement event versioning
- Use priority queues with careful tuning

### 8.3 Async Challenges
- **Callback Hell**: Nested async operations can be hard to read
- **Error Handling**: Try-catch doesn't work across async boundaries
- **Resource Management**: Harder to manage resources with async events

**Mitigation**:
- Use CompletableFuture for composition
- Implement proper error propagation
- Use try-with-resources for cleanup

### 8.4 Performance Overhead
- **Event Objects**: Creating event objects adds GC pressure
- **Thread Coordination**: Concurrent data structures have overhead
- **Message Passing**: More expensive than direct method calls

**Mitigation**:
- Pool event objects where possible
- Use efficient concurrent collections
- Batch events when appropriate

### 8.5 Testing Difficulties
- **Async Testing**: Hard to test async event handling
- **Race Conditions**: Tests may be flaky due to timing
- **Mocking**: Need to mock event bus for unit tests

**Mitigation**:
- Use Awaitility for async testing
- Implement deterministic event scheduling for tests
- Use test doubles for event bus

---

## 9. Complexity Rating: 6/10

### Why 6/10?

**Moderate Complexity Factors**:
- Well-established pattern (pub-sub is common)
- Clear interfaces and contracts
- Good separation of concerns

**Challenging Aspects**:
- Async event handling adds complexity
- Multi-agent coordination is inherently complex
- Error handling in distributed system

### Complexity Breakdown

| Aspect | Complexity | Notes |
|--------|-----------|-------|
| EventBus implementation | 3/10 | Simple pub-sub pattern |
| Event types | 4/10 | Clear event hierarchy |
| Agent communication | 6/10 | Priority queues, filtering |
| Foreman coordination | 7/10 | Task assignment, retries |
| Error handling | 5/10 | Isolation, retries |
| Testing | 7/10 | Async testing challenges |
| Debugging | 6/10 | Event flow tracing |

### Learning Curve

**Beginner** (1-2 weeks):
- Understanding pub-sub pattern
- Event types and their purposes
- Basic subscription/publishing

**Intermediate** (1 month):
- Multi-agent coordination
- Error handling and retries
- Performance tuning

**Advanced** (2-3 months):
- Complex event choreography
- Distributed tracing
- Production debugging

---

## 10. Best Practices

### 10.1 Event Design
- Keep events immutable
- Include timestamps for debugging
- Use meaningful event names
- Include correlation IDs for request tracking

### 10.2 Subscription Management
- Always unsubscribe when done
- Use appropriate priorities
- Handle errors gracefully
- Keep handlers fast

### 10.3 Async Operations
- Use `publishAsync()` for slow handlers
- Avoid blocking in event handlers
- Use CompletableFuture for composition
- Implement proper error propagation

### 10.4 Testing
- Test subscribers in isolation
- Use deterministic event ordering in tests
- Mock event bus for unit tests
- Test error scenarios

### 10.5 Performance
- Batch events when possible
- Use object pooling for high-frequency events
- Monitor queue sizes
- Tune thread pool sizes

---

## 11. Conclusion

The event-driven architecture in MineWright AI's foreman system provides a robust foundation for multi-agent coordination. The loose coupling enables scalability and extensibility, while the event-based communication allows for sophisticated coordination patterns.

The complexity rating of 6/10 reflects the moderate learning curve but acknowledges that the patterns used are well-established and documented. The system successfully balances flexibility with maintainability.

**Key Strengths**:
- Clean separation of concerns
- Easy to extend with new behaviors
- Good observability and debuggability
- Resilient to component failures

**Key Challenges**:
- Async complexity requires careful design
- Event ordering needs consideration
- Testing requires specialized approaches

**Recommendation**: This architecture is well-suited for the multi-agent coordination problem and provides a solid foundation for future enhancements.

---

**Document End**
