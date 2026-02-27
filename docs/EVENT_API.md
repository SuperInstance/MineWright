# MineWright Event API Documentation

This document describes all events published by MineWright. Events enable loose coupling between components through the Observer pattern.

## Table of Contents

1. [Action Events](#action-events)
   - [ActionStartedEvent](#event-actionstartedevent)
   - [ActionCompletedEvent](#event-actioncompletedevent)
2. [State Events](#state-events)
   - [StateTransitionEvent](#event-statetransitionevent)
3. [EventBus API](#eventbus-api)
4. [Built-in Subscribers](#built-in-subscribers)
5. [Creating Custom Events](#creating-custom-events)

---

## Action Events

### Event: `ActionStartedEvent`

Published when an action begins execution.

#### Payload

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `agentId` | String | ID of the agent executing the action | `"foreman-1"` |
| `actionName` | String | Name of the action | `"mine"` |
| `description` | String | Human-readable description | `"Mine 8 iron_ore"` |
| `parameters` | Map<String, Object> | Action parameters | `{"block": "iron_ore", "quantity": 8}` |
| `timestamp` | Instant | When the action started | `2024-01-15T10:30:00Z` |

#### Constructor

```java
public ActionStartedEvent(
    String agentId,
    String actionName,
    String description,
    Map<String, Object> parameters
)
```

#### Publication Triggers

Published when:
- Agent starts executing a task
- `BaseAction.start()` is called
- Action transitions from PLANNING to EXECUTING state

#### Usage Example

```java
eventBus.subscribe(ActionStartedEvent.class, event -> {
    logger.info("Agent {} started action: {}",
        event.getAgentId(),
        event.getActionName());

    // Track active actions
    activeActions.put(event.getAgentId(), event.getActionName());
});
```

#### Version
- **Since**: 1.1.0

---

### Event: `ActionCompletedEvent`

Published when an action finishes execution (success or failure).

#### Payload

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `agentId` | String | ID of the agent | `"foreman-1"` |
| `actionName` | String | Name of the action | `"mine"` |
| `success` | boolean | Whether action succeeded | `true` |
| `message` | String | Result message | `"Mined 8 iron_ore"` |
| `durationMs` | long | Execution duration in milliseconds | `5000` |
| `timestamp` | Instant | When the action completed | `2024-01-15T10:30:05Z` |

#### Constructor

```java
public ActionCompletedEvent(
    String agentId,
    String actionName,
    boolean success,
    String message,
    long durationMs
)
```

#### Publication Triggers

Published when:
- Action completes successfully
- Action fails with error
- Action times out
- Action is cancelled

#### Usage Example

```java
eventBus.subscribe(ActionCompletedEvent.class, event -> {
    if (event.isSuccess()) {
        logger.info("Action {} succeeded in {}ms: {}",
            event.getActionName(),
            event.getDurationMs(),
            event.getMessage());
    } else {
        logger.warn("Action {} failed: {}",
            event.getActionName(),
            event.getMessage());
    }

    // Remove from tracking
    activeActions.remove(event.getAgentId());

    // Update metrics
    metrics.recordActionCompletion(
        event.getActionName(),
        event.isSuccess(),
        event.getDurationMs()
    );
});
```

#### Version
- **Since**: 1.1.0

---

## State Events

### Event: `StateTransitionEvent`

Published when an agent's state changes in the state machine.

#### Payload

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `agentId` | String | ID of the agent | `"foreman-1"` |
| `oldState` | AgentState | Previous state | `AgentState.IDLE` |
| `newState` | AgentState | New state | `AgentState.EXECUTING` |
| `reason` | String | Reason for transition | `"Received command: mine iron"` |
| `timestamp` | Instant | When transition occurred | `2024-01-15T10:30:00Z` |

#### Constructor

```java
public StateTransitionEvent(
    String agentId,
    AgentState oldState,
    AgentState newState,
    String reason
)
```

#### Publication Triggers

Published when:
- Agent receives a new command
- Action completes
- Action fails
- Agent is paused/resumed
- Any state machine transition occurs

#### State Machine States

Valid `AgentState` values:
- `IDLE` - Agent waiting for commands
- `PLANNING` - Processing command with AI
- `EXECUTING` - Performing actions
- `PAUSED` - Temporarily suspended
- `COMPLETED` - All tasks finished
- `FAILED` - Encountered error

#### Usage Example

```java
eventBus.subscribe(StateTransitionEvent.class, event -> {
    logger.info("Agent {} state: {} -> {} (reason: {})",
        event.getAgentId(),
        event.getOldState(),
        event.getNewState(),
        event.getReason());

    // Update UI
    ui.updateAgentState(event.getAgentId(), event.getNewState());

    // Trigger behavior based on state
    if (event.getNewState() == AgentState.FAILED) {
        errorHandler.handleFailure(event.getAgentId(), event.getReason());
    }
});
```

#### Version
- **Since**: 1.1.0

---

## EventBus API

The `EventBus` interface provides publish-subscribe messaging.

### Basic Usage

```java
EventBus eventBus = new SimpleEventBus();
```

### Subscribing to Events

#### Simple Subscription

```java
EventBus.Subscription subscription = eventBus.subscribe(
    ActionStartedEvent.class,
    event -> {
        System.out.println("Action started: " + event.getActionName());
    }
);
```

#### Priority Subscription

Higher priority subscribers are called first.

```java
eventBus.subscribe(
    ActionCompletedEvent.class,
    event -> { /* high priority */ },
    100  // priority (higher = called first)
);
```

#### Unsubscribing

```java
subscription.unsubscribe();

// Or unsubscribe all subscribers for a type
eventBus.unsubscribeAll(ActionStartedEvent.class);
```

### Publishing Events

#### Synchronous Publication

Subscribers are called on the publisher's thread.

```java
eventBus.publish(new ActionStartedEvent(
    "agent-1",
    "mine",
    "Mining iron ore",
    Map.of("block", "iron_ore", "quantity", 8)
));
```

#### Asynchronous Publication

Subscribers are called on a separate thread.

```java
eventBus.publishAsync(new ActionCompletedEvent(
    "agent-1",
    "mine",
    true,
    "Mined 8 iron_ore",
    5000
));
```

### Querying EventBus

```java
// Get subscriber count
int count = eventBus.getSubscriberCount(ActionStartedEvent.class);

// Clear all subscriptions
eventBus.clear();
```

---

## Built-in Subscribers

### LoggingInterceptor

Logs all action lifecycle events.

**Priority**: 0

**Events Subscribed**:
- `ActionStartedEvent`
- `ActionCompletedEvent`
- `StateTransitionEvent`

**Behavior**:
- Logs event details to configured logger
- Includes timestamp, agent ID, and relevant data

### MetricsInterceptor

Tracks execution metrics for monitoring.

**Priority**: 10

**Events Subscribed**:
- `ActionStartedEvent`
- `ActionCompletedEvent`

**Metrics Tracked**:
- Action execution time
- Success/failure rates
- Active action count

### EventPublishingInterceptor

Ensures events are published for every action.

**Priority**: -10 (runs first)

**Events Subscribed**:
- Internal action lifecycle hooks

**Behavior**:
- Publishes events from action execution
- Ensures no action completes without event

---

## Creating Custom Events

### Step 1: Define Event Class

```java
package com.myplugin.events;

import java.time.Instant;
import java.util.Map;

public class CustomEvent {

    private final String agentId;
    private final String customData;
    private final Map<String, Object> metadata;
    private final Instant timestamp;

    public CustomEvent(
        String agentId,
        String customData,
        Map<String, Object> metadata
    ) {
        this.agentId = agentId;
        this.customData = customData;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.timestamp = Instant.now();
    }

    public String getAgentId() {
        return agentId;
    }

    public String getCustomData() {
        return customData;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("CustomEvent{agent='%s', data='%s'}",
            agentId, customData);
    }
}
```

### Step 2: Publish Event

```java
// In your action or component
eventBus.publish(new CustomEvent(
    foreman.getSteveName(),
    "Custom data",
    Map.of("key1", "value1", "key2", 42)
));
```

### Step 3: Subscribe to Event

```java
// In your subscriber component
eventBus.subscribe(CustomEvent.class, event -> {
    logger.info("Received custom event: {}", event);

    // Handle event
    handleCustomEvent(event.getCustomData(), event.getMetadata());
});
```

---

## Event Best Practices

### Do's

1. **Make events immutable** - All fields should be final
2. **Include timestamps** - For debugging and metrics
3. **Use defensive copies** - For collections (use `Map.copyOf()`)
4. **Document payload** - Clear JavaDoc for all properties
5. **Handle exceptions** - Subscribers should catch exceptions
6. **Clean up subscriptions** - Unsubscribe when done

### Don'ts

1. **Don't block** - Subscribers should be fast
2. **Don't modify events** - Events should be immutable
3. **Don't assume order** - Use priority for ordering
4. **Don't publish recursively** - Avoid publishing events in subscribers
5. **Don't ignore errors** - Log and handle exceptions

### Event Naming

Event class names should:
- End with `Event` suffix
- Use past tense for completed actions
- Use present participle for ongoing actions
- Be descriptive

Examples:
- `ActionStartedEvent` (good)
- `ActionStartingEvent` (good)
- `ActionEvent` (too generic)
- `DoActionEvent` (unclear)

### Event Payload Design

**Include**:
- IDs for related entities
- Status flags
- Human-readable messages
- Timestamps
- Essential data only

**Exclude**:
- Large objects
- Entire entity instances
- Cached/duplicatable data
- Non-serializable objects

---

## Event Patterns

### Correlation Pattern

Track related events with correlation ID:

```java
public class CorrelatedEvent {
    private final String correlationId;
    private final String eventId;
    // ... other fields
}
```

### Aggregation Pattern

Aggregate multiple events:

```java
eventBus.subscribe(ActionCompletedEvent.class, event -> {
    recentEvents.add(event);

    if (recentEvents.size() >= BATCH_SIZE) {
        processBatch(recentEvents);
        recentEvents.clear();
    }
});
```

### Filtering Pattern

Filter events before processing:

```java
eventBus.subscribe(ActionCompletedEvent.class, event -> {
    if (!event.isSuccess()) {
        // Only handle failures
        errorHandler.handle(event);
    }
});
```

### State Tracking Pattern

Track state across events:

```java
eventBus.subscribe(ActionStartedEvent.class, event -> {
    stateMap.put(event.getAgentId(), State.RUNNING);
});

eventBus.subscribe(ActionCompletedEvent.class, event -> {
    stateMap.put(event.getAgentId(), State.IDLE);
});
```

---

## Async vs Sync Events

### Synchronous (publish())

**Use when**:
- Subscribers must complete before continuing
- Order of execution matters
- Error handling needs to be immediate

**Example**:
```java
eventBus.publish(new StateTransitionEvent(...));
// Wait for all subscribers to finish before continuing
```

### Asynchronous (publishAsync())

**Use when**:
- Subscribers are slow
- Publisher shouldn't block
- Error handling is separate

**Example**:
```java
eventBus.publishAsync(new MetricsEvent(...));
// Continue immediately, subscribers run on background thread
```

---

## Event Bus Thread Safety

The EventBus implementation is thread-safe:

- **Publishing**: Multiple threads can publish simultaneously
- **Subscribing**: Thread-safe subscription management
- **Notification**: Synchronous notifications are serialized

### Concurrent Subscription

```java
// Thread-safe from multiple threads
CompletableFuture.allOf(
    CompletableFuture.runAsync(() ->
        eventBus.subscribe(EventA.class, handlerA)),
    CompletableFuture.runAsync(() ->
        eventBus.subscribe(EventB.class, handlerB))
).join();
```

---

## Debugging Events

### Enable Event Logging

```java
eventBus.subscribe(Object.class, event -> {
    logger.debug("Event: {} - {}", event.getClass().getSimpleName(), event);
}, Integer.MIN_VALUE); // Lowest priority, sees all events
```

### Trace Event Flow

```java
// Add logging interceptor
eventBus.subscribe(ActionStartedEvent.class, event -> {
    logger.trace("Started: {} by {}", event.getActionName(), event.getAgentId());
});

eventBus.subscribe(ActionCompletedEvent.class, event -> {
    logger.trace("Completed: {} by {} in {}ms",
        event.getActionName(),
        event.getAgentId(),
        event.getDurationMs());
});
```

### Monitor Subscription Count

```java
// Periodic check
scheduledExecutor.scheduleAtFixedRate(() -> {
    logger.info("EventBus subscribers: action-started={}, action-completed={}",
        eventBus.getSubscriberCount(ActionStartedEvent.class),
        eventBus.getSubscriberCount(ActionCompletedEvent.class));
}, 0, 1, TimeUnit.MINUTES);
```
