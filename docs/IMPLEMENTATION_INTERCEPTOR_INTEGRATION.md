# InterceptorChain Integration Implementation Guide

## Overview

This document provides the exact code changes needed to integrate the `InterceptorChain` into the `ActionExecutor`. The analysis reveals that interceptors are created and initialized in the constructor but never called during action execution lifecycle.

## Current State Analysis

### What's Working
- `InterceptorChain` is initialized in `ActionExecutor` constructor (lines 66-73)
- Three interceptors are registered: `LoggingInterceptor`, `MetricsInterceptor`, `EventPublishingInterceptor`
- `ActionContext` contains reference to `InterceptorChain`

### What's Missing
- `interceptorChain.executeBeforeAction()` is never called before starting actions
- `interceptorChain.executeAfterAction()` is never called when actions complete
- `interceptorChain.executeOnError()` is never called on exceptions
- No exception handling around action tick execution for error interception

## Implementation Changes

### File: `src/main/java/com/minewright/action/ActionExecutor.java`

#### Change 1: Wrap `executeTask()` method with interceptor calls

**Location:** Lines 322-338 (current `executeTask` method)

**Current Code:**
```java
private void executeTask(Task task) {
    MineWrightMod.LOGGER.info("Foreman '{}' executing task: {} (action type: {})",
        foreman.getSteveName(), task, task.getAction());

    currentAction = createAction(task);

    if (currentAction == null) {
        String errorMsg = "Unknown action type: " + task.getAction();
        MineWrightMod.LOGGER.error("FAILED to create action for task: {}", task);
        foreman.sendChatMessage("Error: " + errorMsg);
        return;
    }

    MineWrightMod.LOGGER.info("Created action: {} - starting now...", currentAction.getClass().getSimpleName());
    currentAction.start();
    MineWrightMod.LOGGER.info("Action started! Is complete: {}", currentAction.isComplete());
}
```

**New Code:**
```java
private void executeTask(Task task) {
    MineWrightMod.LOGGER.info("Foreman '{}' executing task: {} (action type: {})",
        foreman.getSteveName(), task, task.getAction());

    currentAction = createAction(task);

    if (currentAction == null) {
        String errorMsg = "Unknown action type: " + task.getAction();
        MineWrightMod.LOGGER.error("FAILED to create action for task: {}", task);
        foreman.sendChatMessage("Error: " + errorMsg);
        return;
    }

    MineWrightMod.LOGGER.info("Created action: {} - starting now...", currentAction.getClass().getSimpleName());

    // INTEGRATION POINT 1: Execute beforeAction interceptors
    // If any interceptor returns false, cancel the action
    if (!interceptorChain.executeBeforeAction(currentAction, actionContext)) {
        MineWrightMod.LOGGER.warn("Action cancelled by interceptor: {}", currentAction.getClass().getSimpleName());
        currentAction = null;
        return;
    }

    // Update state machine to executing state
    stateMachine.transitionTo(AgentState.EXECUTING);

    try {
        currentAction.start();
        MineWrightMod.LOGGER.info("Action started! Is complete: {}", currentAction.isComplete());
    } catch (Exception e) {
        // INTEGRATION POINT 3: Execute onError interceptors
        boolean suppressed = interceptorChain.executeOnError(currentAction, e, actionContext);

        if (!suppressed) {
            MineWrightMod.LOGGER.error("Exception during action start", e);
            currentAction = null;
            stateMachine.transitionTo(AgentState.ERROR);
            throw new RuntimeException("Action start failed", e);
        } else {
            MineWrightMod.LOGGER.info("Exception suppressed by interceptor during action start");
            currentAction = null;
            stateMachine.transitionTo(AgentState.IDLE);
        }
    }
}
```

#### Change 2: Add interceptor call in `tick()` when action completes

**Location:** Lines 264-292 (current tick method action completion handling)

**Current Code:**
```java
if (currentAction != null) {
    if (currentAction.isComplete()) {
        ActionResult result = currentAction.getResult();
        MineWrightMod.LOGGER.info("Foreman '{}' - Action completed: {} (Success: {})",
            foreman.getSteveName(), result.getMessage(), result.isSuccess());

        foreman.getMemory().addAction(currentAction.getDescription());

        if (!result.isSuccess()) {
            // Action failed - always notify player via chat
            foreman.sendChatMessage("Action failed: " + result.getMessage());
            if (result.requiresReplanning()) {
                // Also show in GUI if enabled
                if (MineWrightConfig.ENABLE_CHAT_RESPONSES.get()) {
                    sendToGUI(foreman.getSteveName(), "Problem: " + result.getMessage());
                }
            }
        }

        currentAction = null;
    } else {
        if (ticksSinceLastAction % 100 == 0) {
            MineWrightMod.LOGGER.info("Foreman '{}' - Ticking action: {}",
                foreman.getSteveName(), currentAction.getDescription());
        }
        currentAction.tick();
        return;
    }
}
```

**New Code:**
```java
if (currentAction != null) {
    if (currentAction.isComplete()) {
        ActionResult result = currentAction.getResult();
        MineWrightMod.LOGGER.info("Foreman '{}' - Action completed: {} (Success: {})",
            foreman.getSteveName(), result.getMessage(), result.isSuccess());

        foreman.getMemory().addAction(currentAction.getDescription());

        // INTEGRATION POINT 2: Execute afterAction interceptors
        interceptorChain.executeAfterAction(currentAction, result, actionContext);

        // Transition state based on result
        if (result.isSuccess()) {
            stateMachine.transitionTo(AgentState.IDLE);
        } else {
            stateMachine.transitionTo(AgentState.ERROR);
        }

        if (!result.isSuccess()) {
            // Action failed - always notify player via chat
            foreman.sendChatMessage("Action failed: " + result.getMessage());
            if (result.requiresReplanning()) {
                // Also show in GUI if enabled
                if (MineWrightConfig.ENABLE_CHAT_RESPONSES.get()) {
                    sendToGUI(foreman.getSteveName(), "Problem: " + result.getMessage());
                }
            }
        }

        currentAction = null;
    } else {
        if (ticksSinceLastAction % 100 == 0) {
            MineWrightMod.LOGGER.info("Foreman '{}' - Ticking action: {}",
                foreman.getSteveName(), currentAction.getDescription());
        }

        // INTEGRATION POINT 4: Wrap tick() in try-catch for error interception
        try {
            currentAction.tick();
        } catch (Exception e) {
            MineWrightMod.LOGGER.error("Exception during action tick", e);
            boolean suppressed = interceptorChain.executeOnError(currentAction, e, actionContext);

            if (!suppressed) {
                stateMachine.transitionTo(AgentState.ERROR);
                // Re-throw to propagate error
                throw e;
            } else {
                MineWrightMod.LOGGER.info("Exception suppressed by interceptor during tick");
                stateMachine.transitionTo(AgentState.IDLE);
                currentAction = null;
            }
        }
        return;
    }
}
```

#### Change 3: Add interceptor call when cancelling actions

**Location:** Lines 127-136 and 177-185 (action cancellation in command processing)

**Add after existing cancellation logic:**

In `processNaturalLanguageCommand()` (after line 136):
```java
// Cancel any current actions
if (currentAction != null) {
    currentAction.cancel();
    // NEW: Execute afterAction interceptor with cancellation result
    interceptorChain.executeAfterAction(currentAction, currentAction.getResult(), actionContext);
    currentAction = null;
}

if (idleFollowAction != null) {
    idleFollowAction.cancel();
    // NEW: Execute afterAction interceptor with cancellation result
    interceptorChain.executeAfterAction(idleFollowAction, idleFollowAction.getResult(), actionContext);
    idleFollowAction = null;
}

// Reset state machine
stateMachine.transitionTo(AgentState.IDLE);
```

Repeat same pattern for `processNaturalLanguageCommandSync()` (after line 185):
```java
// Cancel any current actions
if (currentAction != null) {
    currentAction.cancel();
    interceptorChain.executeAfterAction(currentAction, currentAction.getResult(), actionContext);
    currentAction = null;
}

if (idleFollowAction != null) {
    idleFollowAction.cancel();
    interceptorChain.executeAfterAction(idleFollowAction, idleFollowAction.getResult(), actionContext);
    idleFollowAction = null;
}

stateMachine.transitionTo(AgentState.IDLE);
```

#### Change 4: Add interceptor call in `stopCurrentAction()`

**Location:** Lines 398-412

**Current Code:**
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
}
```

**New Code:**
```java
public void stopCurrentAction() {
    if (currentAction != null) {
        currentAction.cancel();
        // NEW: Execute afterAction interceptor with cancellation result
        interceptorChain.executeAfterAction(currentAction, currentAction.getResult(), actionContext);
        currentAction = null;
    }
    if (idleFollowAction != null) {
        idleFollowAction.cancel();
        // NEW: Execute afterAction interceptor with cancellation result
        interceptorChain.executeAfterAction(idleFollowAction, idleFollowAction.getResult(), actionContext);
        idleFollowAction = null;
    }
    taskQueue.clear();
    currentGoal = null;

    // Reset state machine
    stateMachine.reset();
}
```

## Testing Checklist

After implementing these changes, verify:

1. **BeforeAction Interception:**
   - [ ] `LoggingInterceptor` logs action start
   - [ ] `MetricsInterceptor` records start time
   - [ ] `EventPublishingInterceptor` publishes ActionStartedEvent
   - [ ] State machine transitions to EXECUTING

2. **AfterAction Interception:**
   - [ ] `LoggingInterceptor` logs action completion
   - [ ] `MetricsInterceptor` records duration
   - [ ] `EventPublishingInterceptor` publishes ActionCompletedEvent
   - [ ] State machine transitions to IDLE or ERROR

3. **OnError Interception:**
   - [ ] Exceptions in `action.start()` are intercepted
   - [ ] Exceptions in `action.tick()` are intercepted
   - [ ] `LoggingInterceptor` logs errors
   - [ ] `MetricsInterceptor` records failure metrics
   - [ ] `EventPublishingInterceptor` publishes ActionFailedEvent
   - [ ] Suppression flag works correctly

4. **Cancellation Handling:**
   - [ ] Cancelled actions trigger `afterAction` interceptor
   - [ ] Idle follow actions trigger interceptors on cancel
   - [ ] State machine properly resets

5. **Edge Cases:**
   - [ ] Null action handling (already exists)
   - [ ] Multiple rapid cancellations
   - [ ] Concurrency during async planning

## Architecture Benefits

Once integrated, the InterceptorChain provides:

1. **Separation of Concerns:** Cross-cutting logic (logging, metrics, events) separated from business logic
2. **Open/Closed Principle:** Add new interceptors without modifying ActionExecutor
3. **Single Responsibility:** ActionExecutor focuses on execution flow, not side effects
4. **Testability:** Each interceptor can be tested independently
5. **Observability:** Built-in logging, metrics, and event publishing

## Verification Commands

```bash
# Build and test
./gradlew build

# Run with debug logging
./gradlew runClient --args="--logging=com.minewright.action=DEBUG"

# Check for interceptor execution in logs
# Should see:
# - "Action cancelled by interceptor" if beforeAction returns false
# - "Exception suppressed by interceptor" if onError returns true
# - Metric collection logs
# - Event publishing logs
```

## Related Files

- `src/main/java/com/minewright/execution/InterceptorChain.java` - Chain implementation
- `src/main/java/com/minewright/execution/ActionInterceptor.java` - Interceptor interface
- `src/main/java/com/minewright/execution/LoggingInterceptor.java` - Example interceptor
- `src/main/java/com/minewright/execution/MetricsInterceptor.java` - Example interceptor
- `src/main/java/com/minewright/execution/EventPublishingInterceptor.java` - Example interceptor
- `src/main/java/com/minewright/execution/ActionContext.java` - Context passed to interceptors
- `src/main/java/com/minewright/execution/AgentStateMachine.java` - State management

## Summary

The integration requires modifications in **4 locations** within `ActionExecutor.java`:

1. **`executeTask()`** - Wrap action start with `executeBeforeAction()` and error handling
2. **`tick()`** - Add `executeAfterAction()` on completion and wrap `tick()` in try-catch
3. **`processNaturalLanguageCommand()`** - Add `executeAfterAction()` when cancelling
4. **`stopCurrentAction()`** - Add `executeAfterAction()` when stopping

All changes maintain backward compatibility while enabling the full interceptor chain functionality.
